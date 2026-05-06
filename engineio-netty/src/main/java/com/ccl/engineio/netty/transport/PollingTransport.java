package com.ccl.engineio.netty.transport;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.exception.SessionNotFoundException;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.core.session.SessionManager;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Engine.IO HTTP 长轮询（Polling）传输处理器
 *
 * <p>负责处理 Engine.IO 基于 HTTP 的请求/响应模式轮询传输：
 * <ul>
 *   <li>POST 请求：客户端发送数据包，服务端解析并传递给下游处理器，响应 200 OK</li>
 *   <li>GET 请求：客户端轮询获取数据，若有缓冲的响应包则立即返回，否则挂起等待（长轮询）</li>
 * </ul>
 * <p>
 * 出站拦截：将下游写入的 {@link EngineIOPacket} 按 Session 缓冲，
 * 而非直接写入 Channel，等待下次 GET 请求时统一编码回传。
 * </p>
 *
 * @see <a href="https://socket.io/docs/v4/engine-io-protocol/#transportpolling">Engine.IO Polling Protocol</a>
 */
public class PollingTransport extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(PollingTransport.class);

    private static final long POST_TIMEOUT = 60_000L;

    // 核心：Session -> 挂起的 GET 请求队列（线程安全）
    private final ConcurrentHashMap<String, Queue<PendingRequest>> sessionPendingGets = new ConcurrentHashMap<>();
    // 可选：每个 Session 待发送的消息队列（无消息时 GET 才会悬挂）
    private final ConcurrentHashMap<String, Queue<String>> sessionPendingMessages = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<PendingRequest>> PENDING_GETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<EngineIOPacket<?>>> OUTPUT_BUFFER = new ConcurrentHashMap<>();

    private final ParserV4 parser;
    private final SessionManager sessionManager;

    public PollingTransport() {
        this.parser = ParserV4.getInstance();
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 1. 校验 HTTP 请求
        if (!request.decoderResult().isSuccess()) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        // 2. 解析 Session ID（从 URL 参数获取）
        String sessionId = getSessionIdFromRequest(request);
        if (sessionId == null || sessionId.isEmpty()) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        // 3. 分方法处理 GET/POST
        handleMessage(ctx, request, sessionId);
    }

    private String getSessionIdFromRequest(FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        return decoder.parameters().getOrDefault("sid", Collections.emptyList()).stream().findFirst().orElse(null);
    }

    private void sendSuccessResponse(ChannelHandlerContext ctx, String content, String origin) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        addOriginHeaders(origin, response);
        HttpUtil.setContentLength(response, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }


    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleMessage(ChannelHandlerContext ctx, FullHttpRequest request, String sid) {
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        if (HttpMethod.GET.equals(request.method())) {
            handleGet(ctx, request, sid, origin);
        } else if (HttpMethod.POST.equals(request.method())) {
            handlePost(ctx, request, sid, origin);
        } else if (HttpMethod.OPTIONS.equals(request.method())) {
            handleOptions(ctx, request, sid);
        } else {
            sendHttpResponse(ctx, request, HttpResponseStatus.METHOD_NOT_ALLOWED);
        }
    }

    private void handlePost(ChannelHandlerContext ctx, FullHttpRequest request, String sessionId, String origin) {
        ByteBuf content = request.content();

        // FullHttpRequest is reference-counted and can be released by upstream.
        // Retain the content since we pass it further down the pipeline.
        content = content.retain();

        log.info("Polling POST {} => {}", origin, content.toString(StandardCharsets.UTF_8));
        // int readableBytes = content.readableBytes();

        // 1. 立即响应 HTTP 200（符合 Polling 协议）
        sendSuccessResponse(ctx, "ok", origin);

        // 2. 解析 POST 数据包（请求体）
        String packet = content.toString(CharsetUtil.UTF_8);

        // 3. 执行业务逻辑（自定义处理 EngineIO 数据包）
        processEngineIOPacket(sessionId, packet);

        // 4. 若有新消息，唤醒挂起的 GET 请求
        wakeupPendingGet(sessionId, origin);
    }

    // 唤醒挂起的 GET：发送待发消息
    private void wakeupPendingGet(String sessionId, String origin) {
        Queue<PendingRequest> pendingGets = sessionPendingGets.get(sessionId);
        Queue<String> pendingMessages = sessionPendingMessages.get(sessionId);
        if (pendingGets == null || pendingGets.isEmpty() || pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        PendingRequest pendingGet = pendingGets.poll();
        String message = pendingMessages.poll();
        sendSuccessResponse(pendingGet.getCtx(), message, origin);
        pendingGet.getPromise().setSuccess();
    }

    // 处理 EngineIO 业务数据包（自定义实现）
    private void processEngineIOPacket(String sessionId, String packet) {
        // 你的业务逻辑：解析消息、存储、转发等
        log.warn("Session[{}] 收到 POST 数据包：{}", sessionId, packet);
    }

    private void addOriginHeaders(String origin, HttpResponse res) {
        if (origin != null) {
            res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
        } else {
            res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }
    }

    private void handleGet(ChannelHandlerContext ctx, FullHttpRequest request, String sessionId, String origin) {
        log.info("Polling GET {} => {}", origin, request.content().toString(StandardCharsets.UTF_8));
        // 1. 获取当前 Session 的待发送消息
        Queue<String> pendingMessages = sessionPendingMessages.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        String message = pendingMessages.poll();

        // 2. 有消息：立即响应，不悬挂
        if (message != null) {
            sendSuccessResponse(ctx, message, origin);
            return;
        }

        // 3. 无消息：悬挂请求，等待超时/消息/升级通知
        // 3.1 创建异步 Promise
        ChannelPromise promise = ctx.newPromise();
        // 3.2 创建超时任务：超时后自动响应空包
        ScheduledFuture<?> timeoutTask = ctx.executor().schedule(() -> {
            onPendingGetTimeout(sessionId, origin);
        }, 60_000, TimeUnit.MILLISECONDS);

        // 3.3 封装 PendingGet 并加入队列
        PendingRequest pendingGet = new PendingRequest(ctx, promise, timeoutTask, sessionId);
        Queue<PendingRequest> queue = sessionPendingGets.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        queue.offer(pendingGet);

        // 3.4 Promise 完成时关闭连接（长轮询单次请求结束）
        promise.addListener(future -> {
            if (!timeoutTask.isDone()) {
                timeoutTask.cancel(false); // 取消超时任务
            }
            ctx.close(); // 关闭连接（Polling 单次请求完成）
        });

    }

    // GET 超时回调：响应空包
    private void onPendingGetTimeout(String sessionId, String origin) {
        Queue<PendingRequest> queue = sessionPendingGets.get(sessionId);
        if (queue == null || queue.isEmpty()) return;

        PendingRequest pendingGet = queue.poll();
        // 超时返回空响应
        sendSuccessResponse(pendingGet.getCtx(), "", origin);
        pendingGet.getPromise().setSuccess();
    }

    private void handleOptions(ChannelHandlerContext ctx, FullHttpRequest request, String sid) {
        // TODO: 2026/04/29 21:28 handleOptions
    }

    private void processPacket(ChannelHandlerContext ctx, String sessionId, EngineIOPacket<?> packet) {
        switch (packet.getType()) {
            case PING:
                if (log.isDebugEnabled()) {
                    log.debug("Polling PING received from session: {}", sessionId);
                }
                EngineIOPacket<Void> pongPacket = EngineIOPacket.of(EngineIOPacket.Type.PONG);
                queuePacket(sessionId, pongPacket);
                sessionManager.updatePingTime(sessionId);
                return;

            case PONG:
                if (log.isDebugEnabled()) {
                    log.debug("Polling PONG received from session: {}", sessionId);
                }
                sessionManager.updatePingTime(sessionId);
                return;

            case CLOSE:
                if (log.isDebugEnabled()) {
                    log.debug("Polling CLOSE received from session: {}", sessionId);
                }
                queuePacket(sessionId, packet);
                ctx.fireChannelRead(packet);
                return;

            default:
                ctx.fireChannelRead(packet);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 连接断开时，清理无效的 PendingGet（可选优化）
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in polling handler for session: {}",
                ctx.channel().attr(ChannelAttributes.SESSION_ID).get(), cause);
        ctx.close();
    }

    private void queuePacket(String sessionId, EngineIOPacket<?> packet) {
        ConcurrentLinkedQueue<EngineIOPacket<?>> buffer = OUTPUT_BUFFER.computeIfAbsent(sessionId,
                key -> new ConcurrentLinkedQueue<>());
        buffer.add(packet);
    }

    private ConcurrentLinkedQueue<PendingRequest> getPendingQueue(String sessionId) {
        return PENDING_GETS.computeIfAbsent(sessionId, key -> new ConcurrentLinkedQueue<>());
    }

    private boolean tryDrainPackets(ChannelHandlerContext ctx, FullHttpRequest request, String sessionId)
            throws Exception {
        if (shouldPing(sessionId)) {
            queuePacket(sessionId, EngineIOPacket.of(EngineIOPacket.Type.PING));
        }

        ConcurrentLinkedQueue<EngineIOPacket<?>> buffer = OUTPUT_BUFFER.get(sessionId);
        if (buffer == null || buffer.isEmpty()) {
            return false;
        }

        drainAndRespond(ctx, request, sessionId);
        return true;
    }

    private void drainAndRespond(ChannelHandlerContext ctx, FullHttpRequest request, String sessionId) {
        ConcurrentLinkedQueue<EngineIOPacket<?>> buffer = OUTPUT_BUFFER.get(sessionId);
        List<EngineIOPacket<?>> packets = new ArrayList<>();
        if (buffer != null) {
            EngineIOPacket<?> packet;
            while ((packet = buffer.poll()) != null) {
                packets.add(packet);
            }
            if (buffer.isEmpty()) {
                OUTPUT_BUFFER.remove(sessionId);
            }
        }

        sendHttpResponse(ctx, request, HttpResponseStatus.OK, packets);
    }

    private boolean shouldPing(String sessionId) {
        ClientContext context = sessionManager.getSession(sessionId);
        long lastPing = context.getLastPingTime();
        long elapsed = System.currentTimeMillis() - lastPing;
        long interval = sessionManager.getPingInterval();

        if (elapsed < interval) {
            return false;
        }

        context.setLastPingTime(System.currentTimeMillis());
        sessionManager.updatePingTime(sessionId);
        return true;
    }

    private boolean isClientDisconnected(ClientContext context) {
        return context != null && !context.isConnected();
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                  HttpResponseStatus status, List<EngineIOPacket<?>> packets) {
        ByteBuf content;
        if (packets != null && !packets.isEmpty()) {
            ByteBuffer payload = parser.encodePayload(packets, true);
            byte[] bytes = new byte[payload.remaining()];
            payload.get(bytes);
            content = Unpooled.wrappedBuffer(bytes);
        } else {
            content = Unpooled.EMPTY_BUFFER;
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        releaseRequest(request);

        ChannelFuture future = ctx.channel().writeAndFlush(response);
        future.addListener((GenericFutureListener<ChannelFuture>) f ->
                ctx.channel().close());
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                  HttpResponseStatus status) {
        sendHttpResponse(ctx, request, status, null);
    }

    private void sendDisconnectResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        List<EngineIOPacket<?>> packets = new ArrayList<>();
        packets.add(EngineIOPacket.of(EngineIOPacket.Type.CLOSE));
        sendHttpResponse(ctx, request, HttpResponseStatus.OK, packets);
    }



    private void releaseRequest(FullHttpRequest request) {
        if (request != null) {
            request.release();
        }
    }

    private static class PendingRequest {
        private final ChannelHandlerContext ctx;
        // 异步响应 Promise
        private final ChannelPromise promise;
        // 超时定时任务
        private final ScheduledFuture<?> timeoutTask;
        // 所属 Session ID
        private final String sessionId;


        public PendingRequest(ChannelHandlerContext ctx, ChannelPromise promise, ScheduledFuture<?> timeoutTask, String sessionId) {
            this.ctx = ctx;
            this.promise = promise;
            this.timeoutTask = timeoutTask;
            this.sessionId = sessionId;
        }

        public ChannelHandlerContext getCtx() {
            return ctx;
        }

        public ChannelPromise getPromise() {
            return promise;
        }

        public ScheduledFuture<?> getTimeoutTask() {
            return timeoutTask;
        }

        public String getSessionId() {
            return sessionId;
        }
    }
}
