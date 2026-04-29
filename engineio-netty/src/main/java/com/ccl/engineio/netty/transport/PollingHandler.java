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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest)) {
            super.channelRead(ctx, msg);
            return;
        }

        FullHttpRequest request = (FullHttpRequest) msg;
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
            sendHttpRequestError(ctx, request, "Invalid session id");
            return;
        }

        if (!sessionManager.hasSession(channelId)) {
            sendHttpRequestError(ctx, request, "No session found");
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
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof EngineIOPacket<?>) {
            EngineIOPacket<?> packet = (EngineIOPacket<?>) msg;
            String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
            if (sessionId != null && sessionManager.hasSession(sessionId)) {
                queuePacket(sessionId, packet);
                promise.setSuccess();
                return;
            }
        }
        super.write(ctx, msg, promise);
    }

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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in polling handler for session: {}",
                ctx.channel().attr(ChannelAttributes.SESSION_ID).get(), cause);
        ctx.close();
    }

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

    private void sendHttpRequestError(ChannelHandlerContext ctx, FullHttpRequest request, String message) {
        ByteBuf content = Unpooled.copiedBuffer(message, java.nio.charset.StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.BAD_REQUEST, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        releaseRequest(request);
        ctx.writeAndFlush(response).addListener((GenericFutureListener<ChannelFuture>) f ->
                ctx.channel().close());
    }

    private void releaseRequest(FullHttpRequest request) {
        if (request != null) {
            request.release();
        }
    }

    private static class PendingRequest {
        private final ChannelHandlerContext ctx;
        private final FullHttpRequest request;

        PendingRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
            this.ctx = ctx;
            this.request = request;
        }

        void cancel() {
            if (request != null) {
                request.release();
            }
        }
    }
}
