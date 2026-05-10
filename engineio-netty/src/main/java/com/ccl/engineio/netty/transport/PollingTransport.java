package com.ccl.engineio.netty.transport;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.core.parser.Parser;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.core.session.SessionManager;
import com.ccl.engineio.netty.EngineMessage;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.engineio.netty.handler.CorsUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

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
    private static final ConcurrentHashMap<String, Queue<PendingRequest>> PENDING_GETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Queue<ByteBuf>> OUTPUT_PACKETS = new ConcurrentHashMap<>();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
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

    private void sendSuccessResponse(ChannelHandlerContext ctx, ByteBuf content, String origin) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        addOriginHeaders(origin, response);
        HttpUtil.setContentLength(response, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
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
        ctx.channel().attr(ChannelAttributes.SESSION_ID).set(sid);
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        if (HttpMethod.GET.equals(request.method())) {
            handleGet(ctx, request, sid, origin);
        } else if (HttpMethod.POST.equals(request.method())) {
            handlePost(ctx, request, sid, origin);
        } else if (HttpMethod.OPTIONS.equals(request.method())) {
            handleOptions(ctx, sid, origin);
        } else {
            sendErrorResponse(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
        }
    }

    private void handlePost(ChannelHandlerContext ctx, FullHttpRequest request, String sessionId, String origin) {
        ByteBuf content = request.content();
        if (log.isTraceEnabled()) {
            log.trace("POST {} => {}", request.uri(), content.toString(StandardCharsets.UTF_8));
        }
        // FullHttpRequest is reference-counted and can be released by upstream.
        // Retain the content since we pass it further down the pipeline.
        content = content.retain();

        // 1. 立即响应 HTTP 200（符合 Polling 协议）
        sendSuccessResponse(ctx, "ok", origin);

        // 2. 传递消息到下一个处理器
        ClientContext client = SessionManager.getInstance().getSession(sessionId);
        EngineMessage message = EngineMessage.builder().client(client)
                .content(content).transport(TransportType.POLLING).build();
        ctx.fireChannelRead(message);

        // 3. 若有新消息，唤醒挂起的 GET 请求
        wakeupPendingGet(sessionId, origin);
    }

    /**
     * 唤醒挂起的 GET 请求，发送待发消息
     *
     * <p>取出最早挂起的 GET 请求，将当前 Session 的所有待发消息写入其 Channel。</p>
     *
     * @param sessionId 会话 ID
     * @param origin    请求来源
     */
    private void wakeupPendingGet(String sessionId, String origin) {
        Queue<PendingRequest> pendingGets = PENDING_GETS.get(sessionId);
        Queue<ByteBuf> pendingMessages = OUTPUT_PACKETS.get(sessionId);
        if (pendingGets == null || pendingGets.isEmpty() || pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        PendingRequest pendingGet = pendingGets.poll();

        ByteBuf message;
        // 非阻塞循环：取一条 → 处理一条 → 直到队列为null
        while ((message = pendingMessages.poll()) != null) {
            sendSuccessResponse(pendingGet.getCtx(), message, origin);
        }

        pendingGet.getPromise().setSuccess();
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
        if (log.isTraceEnabled()) {
            log.trace("GET {}", request.uri());
        }
        // 1. 获取当前 Session 的待发送消息
        Queue<ByteBuf> pendingMessages = OUTPUT_PACKETS.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        ByteBuf message = pendingMessages.poll();

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
        Queue<PendingRequest> queue = PENDING_GETS.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        queue.offer(pendingGet);

        // 3.4 Promise 完成时关闭连接（长轮询单次请求结束）
        promise.addListener(future -> {
            if (!timeoutTask.isDone()) {
                timeoutTask.cancel(false); // 取消超时任务
            }
            ctx.close(); // 关闭连接（Polling 单次请求完成）
        });

    }

    /**
     * GET 超时回调，响应空包
     *
     * @param sessionId 会话 ID
     * @param origin    请求来源
     */
    private void onPendingGetTimeout(String sessionId, String origin) {
        Queue<PendingRequest> queue = PENDING_GETS.get(sessionId);
        if (queue == null || queue.isEmpty()) return;

        PendingRequest pendingGet = queue.poll();
        // TODO: 2026/05/08 16:19 超时返回空响应
        sendSuccessResponse(pendingGet.getCtx(), "", origin);
        pendingGet.getPromise().setSuccess();
    }

    private void handleOptions(ChannelHandlerContext ctx, String sid, String origin) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
        CorsUtil.addCorsHeaders(response, origin);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, 86400);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

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


    public final void sendMessage(String sid, ByteBuf content) {
        if (log.isTraceEnabled()) {
            log.trace("OUT {}", content.toString(CharsetUtil.UTF_8));
        }
        Queue<ByteBuf> queue = OUTPUT_PACKETS.get(sid);
        queue.offer(content);
        wakeupPendingGet(sid, "*");
    }

    public static class PendingRequest {
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
