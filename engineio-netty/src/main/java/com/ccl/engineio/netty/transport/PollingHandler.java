package com.ccl.engineio.netty.transport;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.exception.SessionNotFoundException;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.DataType;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.core.session.SessionManager;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Engine.IO HTTP 长轮询（Polling）传输处理器
 * <p>
 * 负责处理 Engine.IO 基于 HTTP 的请求/响应模式轮询传输：
 * <ul>
 *   <li>POST 请求：客户端发送数据包，服务端解析并传递给下游处理器，响应 200 OK</li>
 *   <li>GET 请求：客户端轮询获取数据，若有缓冲的响应包则立即返回，否则挂起等待（长轮询）</li>
 * </ul>
 * </p>
 * <p>
 * 出站拦截：将下游写入的 {@link EngineIOPacket} 按 Session 缓冲，
 * 而非直接写入 Channel，等待下次 GET 请求时统一编码回传。
 * </p>
 *
 * @see <a href="https://socket.io/docs/v4/engine-io-protocol/#transportpolling">Engine.IO Polling Protocol</a>
 */
public class PollingHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PollingHandler.class);

    private static final long POST_TIMEOUT = 60_000L;

    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<EngineIOPacket<?>>> OUTPUT_BUFFER = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<PendingRequest>> PENDING_GETS = new ConcurrentHashMap<>();

    private final ParserV4 parser;
    private final SessionManager sessionManager;

    public PollingHandler() {
        this.parser = ParserV4.getInstance();
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * 入站：处理 HTTP 请求，根据方法类型分发至 POST 或 GET 处理逻辑
     *
     * @param ctx  通道上下文
     * @param msg  入站消息，仅处理 {@link FullHttpRequest}
     * @throws Exception 处理异常
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest request)) {
            super.channelRead(ctx, msg);
            return;
        }

        QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = queryDecoder.parameters();
        List<String> sidValues = params.get("sid");

        if (sidValues == null || sidValues.isEmpty()) {
            ctx.fireChannelRead(msg);
            return;
        }

        String channelId = sidValues.get(0);
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId == null || !sessionId.equals(channelId)) {
            sendHttpRequestError(ctx, "Invalid session id");
            request.release();
            return;
        }

        if (!sessionManager.hasSession(channelId)) {
            sendHttpRequestError(ctx, "No session found");
            request.release();
            return;
        }

        List<String> transportValues = params.get("transport");
        boolean isPolling = true;
        if (transportValues != null && !transportValues.isEmpty()) {
            try {
                isPolling = TransportType.valueOf(transportValues.get(0).toUpperCase()) == TransportType.POLLING;
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (!isPolling) {
            super.channelRead(ctx, msg);
            return;
        }

        String method = request.method().name();
        if ("POST".equalsIgnoreCase(method)) {
            handlePost(ctx, channelId, request);
        } else if ("GET".equalsIgnoreCase(method)) {
            handleGet(ctx, channelId, request);
        } else {
            sendHttpResponse(ctx, request, HttpResponseStatus.METHOD_NOT_ALLOWED);
            request.release();
        }
    }

    /**
     * 出站：拦截 {@link EngineIOPacket}，将其缓冲至 Session 队列，等待下次 GET 请求时一并返回
     *
     * @param ctx     通道上下文
     * @param msg     出站消息
     * @param promise 通道操作承诺
     * @throws Exception 处理异常
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof EngineIOPacket<?> packet) {
            String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
            if (sessionId != null && sessionManager.hasSession(sessionId)) {
                queuePacket(sessionId, packet);
                promise.setSuccess();
                return;
            }
        }
        super.write(ctx, msg, promise);
    }

    /**
     * 处理 POST 请求：读取请求体中的 Engine.IO 数据包，解码后传递给下游处理器
     *
     * @param ctx       通道上下文
     * @param sessionId 会话 ID
     * @param request   HTTP 请求
     */
    private void handlePost(ChannelHandlerContext ctx, String sessionId, FullHttpRequest request) {
        ByteBuf content = request.content();
        int readableBytes = content.readableBytes();

        if (readableBytes > 0) {
            byte[] bodyBytes = new byte[readableBytes];
            content.getBytes(content.readerIndex(), bodyBytes);

            try {
                List<EngineIOPacket<?>> packets = parser.decodePayload(bodyBytes, DataType.PLAINTEXT);
                for (EngineIOPacket<?> packet : packets) {
                    processPacket(ctx, sessionId, packet);
                }
            } catch (Exception e) {
                log.error("Failed to decode polling POST payload for session: {}", sessionId, e);
            }
        }

        drainAndRespond(ctx, request, sessionId);
    }

    /**
     * 处理 GET 请求：检查并返回缓冲的响应包，无数据时启动长轮询等待
     *
     * @param ctx       通道上下文
     * @param sessionId 会话 ID
     * @param request   HTTP 请求
     */
    private void handleGet(ChannelHandlerContext ctx, String sessionId, FullHttpRequest request) {
        ClientContext context = sessionManager.getSession(sessionId);
        ConcurrentLinkedQueue<PendingRequest> pendingQueue = getPendingQueue(sessionId);
        PendingRequest pending = new PendingRequest(ctx, request);
        pendingQueue.add(pending);

        try {
            boolean drained = tryDrainPackets(ctx, request, sessionId);
            if (drained) {
                return;
            }
        } catch (Exception e) {
            pendingQueue.remove(pending);
            log.error("Error draining packets for session: {}", sessionId, e);
        }

        if (isClientDisconnected(context)) {
            pendingQueue.remove(pending);
            sendDisconnectResponse(ctx, request);
            return;
        }

        long longPollTimeout = Math.max(POST_TIMEOUT, sessionManager.getPingInterval() + sessionManager.getPingTimeout());
        scheduleLongPollTimeout(ctx, sessionId, pending, longPollTimeout, pendingQueue);
    }

    /**
     * 处理单个数据包：根据包类型执行不同的协议逻辑，协议包就地响应，业务包传递给下游
     *
     * @param ctx       通道上下文
     * @param sessionId 会话 ID
     * @param packet    待处理的 Engine.IO 数据包
     */
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

    /**
     * 通道非激活时清理：取消 Session 下所有待处理的长轮询请求
     *
     * @param ctx 通道上下文
     * @throws Exception 处理异常
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            ConcurrentLinkedQueue<PendingRequest> pendingQueue = PENDING_GETS.get(sessionId);
            if (pendingQueue != null) {
                PendingRequest pending;
                while ((pending = pendingQueue.poll()) != null) {
                    pending.cancel();
                }
            }
            OUTPUT_BUFFER.remove(sessionId);
            PENDING_GETS.remove(sessionId);
        }
        super.channelInactive(ctx);
    }

    /**
     * 异常捕获：记录错误并关闭通道
     *
     * @param ctx   通道上下文
     * @param cause 异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in polling handler for session: {}",
                ctx.channel().attr(ChannelAttributes.SESSION_ID).get(), cause);
        ctx.close();
    }

    /**
     * 调度长轮询超时任务：在指定超时后重新检查缓冲队列，若仍无数据则循环调度直至超时上限
     *
     * @param ctx          通道上下文
     * @param sessionId    会话 ID
     * @param pending      待处理请求对象
     * @param totalTimeout 总超时时间限制（毫秒）
     * @param pendingQueue 待处理请求队列
     */
    private void scheduleLongPollTimeout(ChannelHandlerContext ctx, String sessionId,
                                         PendingRequest pending, long totalTimeout,
                                         ConcurrentLinkedQueue<PendingRequest> pendingQueue) {
        long checkInterval = Math.min(25_000L, totalTimeout);

        ctx.executor().schedule(() -> {
            boolean removed = pendingQueue.remove(pending);
            if (!removed) {
                return;
            }

            try {
                boolean drained = tryDrainPackets(ctx, pending.request, sessionId);
                if (drained) {
                    return;
                }

                ClientContext context = sessionManager.getSession(sessionId);
                if (isClientDisconnected(context)) {
                    sendDisconnectResponse(ctx, pending.request);
                    return;
                }

                long remaining = totalTimeout - checkInterval;
                if (remaining > 0) {
                    scheduleLongPollTimeout(ctx, sessionId, pending, remaining, pendingQueue);
                } else {
                    drainAndRespond(ctx, pending.request, sessionId);
                }
            } catch (SessionNotFoundException e) {
                releaseRequest(pending.request);
            } catch (Exception e) {
                log.error("Error in long-polling timeout for session: {}", sessionId, e);
                releaseRequest(pending.request);
            }
        }, checkInterval, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 将数据包加入指定 Session 的输出缓冲队列
     *
     * @param sessionId 会话 ID
     * @param packet    待缓冲的 Engine.IO 数据包
     */
    private void queuePacket(String sessionId, EngineIOPacket<?> packet) {
        ConcurrentLinkedQueue<EngineIOPacket<?>> buffer = OUTPUT_BUFFER.computeIfAbsent(sessionId,
                key -> new ConcurrentLinkedQueue<>());
        buffer.add(packet);
    }

    /**
     * 获取指定 Session 的长轮询请求队列，不存在时自动创建
     *
     * @param sessionId 会话 ID
     * @return 待处理请求队列
     */
    private ConcurrentLinkedQueue<PendingRequest> getPendingQueue(String sessionId) {
        return PENDING_GETS.computeIfAbsent(sessionId, key -> new ConcurrentLinkedQueue<>());
    }

    /**
     * 尝试检查是否需要发送心跳 PING，并检查缓冲队列中是否有数据可立即返回
     *
     * @param ctx       通道上下文
     * @param request   HTTP 请求
     * @param sessionId 会话 ID
     * @return true 表示已发送响应，false 表示无数据可供响应
     * @throws Exception 编码或发送异常
     */
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

    /**
     * 排空指定 Session 的输出缓冲队列，编码为 HTTP 响应并发送
     *
     * @param ctx       通道上下文
     * @param request   当前的 HTTP 请求
     * @param sessionId 会话 ID
     */
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

    /**
     * 检查指定 Session 是否已到达心跳 PING 发送时机
     *
     * @param sessionId 会话 ID
     * @return true 表示需要发送 PING
     */
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

    /**
     * 判断客户端是否已断开连接
     *
     * @param context 客户端上下文
     * @return true 表示客户端已断开
     */
    private boolean isClientDisconnected(ClientContext context) {
        return context != null && !context.isConnected();
    }

    /**
     * 构建并发送 HTTP 响应，包含可选的 Engine.IO 数据包负载
     *
     * @param ctx       通道上下文
     * @param request   原始请求
     * @param status    HTTP 状态码
     * @param packets   待编码的 Engine.IO 数据包列表，为 null 时发送空响应体
     */
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

    /**
     * 发送简单状态的 HTTP 响应（空响应体）
     *
     * @param ctx    通道上下文
     * @param request   原始请求
     * @param status HTTP 状态码
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                  HttpResponseStatus status) {
        sendHttpResponse(ctx, request, status, null);
    }

    /**
     * 发送断开连接的响应：携带 CLOSE 类型数据包后关闭连接
     *
     * @param ctx     通道上下文
     * @param request 原始请求
     */
    private void sendDisconnectResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        List<EngineIOPacket<?>> packets = new ArrayList<>();
        packets.add(EngineIOPacket.of(EngineIOPacket.Type.CLOSE));
        sendHttpResponse(ctx, request, HttpResponseStatus.OK, packets);
    }

    /**
     * 发送 HTTP 错误响应（400 Bad Request）
     *
     * @param ctx     通道上下文
     * @param message 错误消息文本
     */
    private void sendHttpRequestError(ChannelHandlerContext ctx, String message) {
        ByteBuf content = Unpooled.copiedBuffer(message, java.nio.charset.StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.BAD_REQUEST, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        ctx.writeAndFlush(response).addListener((GenericFutureListener<ChannelFuture>) f ->
                ctx.channel().close());
    }

    /**
     * 安全释放 {@link FullHttpRequest} 引用计数
     *
     * @param request 待释放的 HTTP 请求
     */
    private void releaseRequest(FullHttpRequest request) {
        if (request != null) {
            request.release();
        }
    }

    /**
     * 封装长轮询中的待处理请求，持有上下文和原始请求引用
     */
    private static class PendingRequest {
        private final ChannelHandlerContext ctx;
        private final FullHttpRequest request;

        PendingRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
            this.ctx = ctx;
            this.request = request;
        }

        /**
         * 取消待处理请求：释放持有的 HTTP 请求引用
         */
        void cancel() {
            if (request != null) {
                request.release();
            }
        }
    }
}
