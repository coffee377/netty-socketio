package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.core.entity.OpenData;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.EngineVersion;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.core.session.SessionManager;

import io.netty.channel.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Engine.IO 握手协议处理器
 *
 * <p>仅处理 Engine.IO 协议握手阶段：
 * <ul>
 *   <li>验证请求路径和 transport 参数</li>
 *   <li>创建 Session 并生成 sessionId</li>
 *   <li>发送 OPEN 响应包（含连接参数）</li>
 *   <li>WebSocket 模式动态添加协议处理器</li>
 * </ul>
 * <p>
 * 握手完成后，数据传输由其他 Handler 负责。
 */
public class EngineIOHandshakeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(EngineIOHandshakeHandler.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String connectPath;
    private final boolean enableCors;
    private final String corsOrigin;
    private final SessionManager sessionManager;
    private final int maxFramePayloadLength;
    private final ParserV4 parser;

    public EngineIOHandshakeHandler(String connectPath, int maxFramePayloadLength) {
        this(connectPath, maxFramePayloadLength, true, "*");
    }

    public EngineIOHandshakeHandler(String connectPath, int maxFramePayloadLength,
                                    boolean enableCors, String corsOrigin) {
        this.connectPath = connectPath;
        this.sessionManager = SessionManager.getInstance();
        this.maxFramePayloadLength = maxFramePayloadLength;
        this.enableCors = enableCors;
        this.corsOrigin = corsOrigin;
        this.parser = ParserV4.getInstance();
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return super.acceptInboundMessage(msg) && isV4Handshake((FullHttpRequest) msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        handleHandshake(ctx, req, "polling", null);
    }

    // 辅助方法：判断是否为EngineIO握手请求
    private boolean isV4Handshake(FullHttpRequest req) {
        QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());
        String path = queryDecoder.path();

        String sid = getQueryParam(queryDecoder, "sid");
        String transport = getQueryParam(queryDecoder, "transport");
        String version = getQueryParam(queryDecoder, EngineVersion.EIO);
        return path.startsWith(connectPath) && HttpMethod.GET.equals(req.method()) &&
                TransportType.POLLING.getName().equals(transport) &&
                sid == null && EngineVersion.V4.getStrValue().equals(version);
    }

    private String getQueryParam(QueryStringDecoder queryDecoder, String name) {
        List<String> values = queryDecoder.parameters().get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().findFirst().orElse(null);
    }

//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        if (msg instanceof FullHttpRequest) {
//            FullHttpRequest request = (FullHttpRequest) msg;
//            QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
//
//            String path = queryDecoder.path();
//            if (!path.startsWith(connectPath)) {
//                if (log.isWarnEnabled()) {
//                    log.warn("rejecting invalid path request: {} from client: {}", path, ctx.channel().remoteAddress());
//                }
//                sendHttpResponse(ctx, request, HttpResponseStatus.BAD_REQUEST);
//                return;
//            }
//
//            Map<String, List<String>> parameters = queryDecoder.parameters();
//
//            List<String> sids = parameters.get("sid");
//            if (sids != null && sids.get(0) != null) {
//                ClientContext session = sessionManager.getSession(sids.get(0));
//                session.setTransportType(TransportType.POLLING);
//                ctx.channel().attr(ChannelAttributes.SESSION_ID).set(session.getSessionId());
//                ctx.fireChannelRead(request);
//                return;
//            }
//
//            String transport = parameters.get("transport").stream().findFirst().orElse(null);
//            handleHandshake(ctx, request, transport, parameters);
//            return;
//        }
//
//        super.channelRead(ctx, msg);
//    }

    private void handleHandshake(ChannelHandlerContext ctx, FullHttpRequest request,
                                 String transport, Map<String, List<String>> params) {
        TransportType transportType = TransportType.of(transport);
        if (transportType == null) {
            if (log.isDebugEnabled()) {
                log.debug("Missing transport parameter for client: {}", ctx.channel().remoteAddress());
            }
            sendHttpRequestError(ctx, request, "Invalid transport");
        }

        ClientContext clientContext = sessionManager.createSession(transportType);
        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (clientAddress != null) {
            clientContext.setRemoteAddress(clientAddress.getAddress().toString());
        }
        ctx.channel().attr(ChannelAttributes.SESSION_ID).set(clientContext.getSessionId());
        if (log.isDebugEnabled()) {
            log.debug("Handshake successful for client: {} with session: {}",
                    ctx.channel().remoteAddress(), clientContext.getSessionId());
        }

        if (TransportType.WEBSOCKET.equals(transportType)) {
            handshake(ctx, clientContext.getSessionId(), request.uri(), request);
        } else if (TransportType.POLLING.equals(transportType)) {
            OpenData openData = new OpenData();
            openData.setSid(clientContext.getSessionId());
            List<String> upgrades = Collections.singletonList(TransportType.WEBSOCKET.getName());
            openData.setUpgrades(upgrades);
            openData.setPingInterval((int) sessionManager.getPingInterval());
            openData.setPingTimeout((int) sessionManager.getPingTimeout());
            openData.setMaxPayload(maxFramePayloadLength);

            EngineIOPacket<String> packet;

            try {
                String json = OBJECT_MAPPER.writeValueAsString(openData);
                packet = EngineIOPacket.of(EngineIOPacket.Type.OPEN, json);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize OpenData", e);
                sendHttpResponse(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                return;
            }

            byte[] bytes = parser.encodePacket(packet, true);
            ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
            if (log.isInfoEnabled()) {
                log.info("{}", byteBuf.toString(StandardCharsets.UTF_8));
            }

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    byteBuf);

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
            response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
            addCorsHeaders(response);

            ctx.writeAndFlush(response);
//                    .addListener(ChannelFutureListener.CLOSE);
        }

    }

    private String getWebSocketLocation(HttpRequest req, boolean ssl) {
        String protocol = "ws://";
        if (ssl) {
            protocol = "wss://";
        }
        return protocol + req.headers().get(HttpHeaderNames.HOST) + req.uri();
    }

    private void handshake(ChannelHandlerContext ctx, final String sessionId, String path, FullHttpRequest req) {
        final Channel channel = ctx.channel();

        // RFC 6455 reserves the RSV bits for negotiated WebSocket extensions (not only compression).
        // In this server, extension handling is effectively tied to WebSocketServerCompressionHandler,
        // which is installed only when websocketCompression is enabled. Keep the handshake's
        // allowExtensions flag consistent with that to avoid negotiating extensions the pipeline
        // won't actually handle.
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req, false),
                null, true, 64 * 1024);
        WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
        if (handshaker != null) {
            try {
                ChannelFuture f = handshaker.handshake(channel, req);
                f.addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        log.error("Can't handshake {}", sessionId, future.cause());
                        closeClient(sessionId, channel);
                        return;
                    }
                    channel.pipeline().addBefore("wsUpgrade", "webSocketAggregator",
                            new WebSocketFrameAggregator(64 * 1024));
                    sendOpenPacket(sessionId, ctx);
                    connectClient(channel, sessionId);
                });
            } catch (Exception e) {
                log.warn("Can't handshake {}, {}", sessionId, e.getMessage(), e);
                closeClient(sessionId, channel);
            }
        } else {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }
    }

    private void connectClient(Channel channel, String sessionId) {

    }

    private void closeClient(String sessionId, Channel channel) {
        try {
            channel.close();
        } catch (Exception t) {
            log.warn("Can't close channel for sessionId: {}", sessionId, t);
        }
//        ClientHead clientHead = clientsBox.get(sessionId);
//        if (clientHead != null && clientHead.getNamespaces().isEmpty()) {
//            clientsBox.removeClient(sessionId);
//            clientHead.disconnect();
//        }
        log.info("Client with sessionId: {} was disconnected", sessionId);
    }

    private void addWebSocketProtocolHandler(ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("Adding WebSocketServerProtocolHandler to pipeline for: {}", ctx.channel().remoteAddress());
        }
        ctx.pipeline().addBefore(ctx.name(), "wsProtocol",
                new WebSocketServerProtocolHandler(null, true, maxFramePayloadLength));
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status) {
//        if (log.isDebugEnabled()) {
//            log.debug("Sending HTTP error response: {} to client: {}", status, ctx.channel().remoteAddress());
//        }
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private void sendHttpRequestError(ChannelHandlerContext ctx, FullHttpRequest req, String message) {
//        if (log.isDebugEnabled()) {
//            log.debug("Sending WebSocket error frame to client: {} with message: {}", ctx.channel().remoteAddress(), message);
//        }
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
//        response.content().writeBytes(message.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void addCorsHeaders(FullHttpResponse response) {
        if (enableCors) {
            if (corsOrigin != null && !corsOrigin.isEmpty()) {
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, corsOrigin);
                if (!"*".equals(corsOrigin)) {
                    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
                }
            }
        }
    }

    private void sendClosePacket(ChannelHandlerContext ctx, FullHttpRequest request) {
        EngineIOPacket<String> packet = EngineIOPacket.of(EngineIOPacket.Type.CLOSE);
        byte[] bytes = parser.encodePacket(packet, false);
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        addCorsHeaders(response);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        request.release();
    }

    private void sendOpenPacket(String sessionId, ChannelHandlerContext ctx) {
        ClientContext clientContext = sessionManager.getSession(sessionId);
        if (clientContext == null) {
            log.warn("Session not found for Open packet: {}", sessionId);
            return;
        }

        OpenData openData = new OpenData();
        openData.setSid(sessionId);
        openData.setUpgrades(Collections.singletonList(TransportType.WEBSOCKET.getName()));
        openData.setPingInterval((int) sessionManager.getPingInterval());
        openData.setPingTimeout((int) sessionManager.getPingTimeout());
        openData.setMaxPayload(maxFramePayloadLength);

        EngineIOPacket<String> packet;
        try {
            String json = OBJECT_MAPPER.writeValueAsString(openData);
            packet = EngineIOPacket.of(EngineIOPacket.Type.OPEN, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OpenData", e);
            return;
        }

        byte[] bytes = parser.encodePacket(packet, true);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        if (log.isDebugEnabled()) {
            log.debug("Sending OPEN packet to session: {}", sessionId);
        }

        ctx.writeAndFlush(byteBuf);
    }

}
