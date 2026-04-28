package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.entity.OpenData;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.core.session.SessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Engine.IO 握手处理器
 * 处理客户端首次连接请求，创建 Session，发送 OPEN 响应
 */
public class EngineIOHandshakeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(EngineIOHandshakeHandler.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final AttributeKey<FullHttpRequest> CONNECTED_HTTP_REQUEST = AttributeKey.valueOf("connectedHttpRequest");

    private final String connectPath;
    private final SessionManager sessionManager;
    private final int maxFramePayloadLength;
    private final boolean enableCors;
    private final String corsOrigin;
    private final ParserV4 parser;

    public EngineIOHandshakeHandler(String connectPath, int maxFramePayloadLength) {
        this(connectPath, maxFramePayloadLength, true, "*");
    }

    public EngineIOHandshakeHandler(String connectPath, int maxFramePayloadLength, boolean enableCors, String corsOrigin) {
        this.connectPath = connectPath;
        this.sessionManager = SessionManager.getInstance();
        this.maxFramePayloadLength = maxFramePayloadLength;
        this.enableCors = enableCors;
        this.corsOrigin = corsOrigin;
        this.parser = ParserV4.getInstance();
    }

    /**
     * 处理握手请求
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest fullHttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }

        QueryStringDecoder queryDecoder = new QueryStringDecoder(fullHttpRequest.uri());
        String path = queryDecoder.path();

        if (!path.startsWith(connectPath)) {
            if (log.isDebugEnabled()) {
                log.debug("rejecting invalid path request: {} from client: {}", path, ctx.channel().remoteAddress());
            }
            sendHttpResponse(ctx, fullHttpRequest, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        Map<String, List<String>> params = queryDecoder.parameters();
        List<String> sidValues = params.get("sid");
        List<String> transportValues = params.get("transport");

        if (sidValues == null || sidValues.isEmpty()) {
            if (!handleHandshake(ctx, fullHttpRequest, params, transportValues)) {
                fullHttpRequest.release();
                return;
            }

            boolean isWebSocket = transportValues != null && !transportValues.isEmpty()
                    && transportValues.get(0).equalsIgnoreCase("websocket");

            if (isWebSocket) {
                addWebSocketProtocolHandler(ctx);
            }
        } else {
            ctx.channel().attr(ChannelAttributes.SESSION_ID).set(sidValues.get(0));
        }

//        ctx.channel().attr(CONNECTED_HTTP_REQUEST).set(fullHttpRequest);
        ctx.fireChannelRead(msg);
    }

    /**
     * 处理握手逻辑
     */
    private boolean handleHandshake(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest,
                                    Map<String, List<String>> params, List<String> transports) {
        if (log.isDebugEnabled()) {
            log.debug("Processing handshake request from client: {}", ctx.channel().remoteAddress());
        }

        String transport = transports != null ? transports.get(0) : null;
        if (transport == null || transport.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Missing transport parameter for client: {}", ctx.channel().remoteAddress());
            }
            sendHttpResponse(ctx, fullHttpRequest, HttpResponseStatus.BAD_REQUEST);
            return false;
        }

        TransportType transportType = TransportType.POLLING;
        try {
            transportType = TransportType.valueOf(transport.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendHttpRequestError(ctx, fullHttpRequest, "Invalid transport");
            return false;
        }

        String sessionId = sessionManager.createSession();
        com.ccl.engineio.core.entity.ClientContext clientContext = sessionManager.getSession(sessionId);
        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (clientAddress != null) {
            clientContext.setRemoteAddress(clientAddress.getAddress().toString());
        }
        clientContext.setTransportType(transportType);
        ctx.channel().attr(ChannelAttributes.SESSION_ID).set(sessionId);

        if (log.isDebugEnabled()) {
            log.debug("Handshake successful for client: {} with session: {}", ctx.channel().remoteAddress(), sessionId);
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
            sendHttpResponse(ctx, fullHttpRequest, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return false;
        }

        byte[] bytes = parser.encodePacket(packet, true);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        log.info("{}",byteBuf.toString(StandardCharsets.UTF_8));
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                byteBuf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");

        // 添加 CORS 响应头
        if (enableCors) {
            if (corsOrigin != null && !corsOrigin.isEmpty()) {
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, corsOrigin);
                if (!"*".equals(corsOrigin)) {
                    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
                }
            }
        }

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        return true;
    }

    /**
     * 动态添加 WebSocket 协议处理器
     * <p>
     * 当 transport=websocket 时，需要向 pipeline 中插入 WebSocketServerProtocolHandler，
     * 使其在 handshake handler 之后、codec 之前工作。
     * </p>
     */
    private void addWebSocketProtocolHandler(ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("Adding WebSocketServerProtocolHandler to pipeline for: {}", ctx.channel().remoteAddress());
        }
        ctx.pipeline().addBefore(ctx.name(), "wsProtocol",
                new WebSocketServerProtocolHandler(null, true, maxFramePayloadLength));
    }

    /**
     * 发送 HTTP 错误响应
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status) {
        if (log.isDebugEnabled()) {
            log.debug("Sending HTTP error response: {} to client: {}", status, ctx.channel().remoteAddress());
        }
        FullHttpResponse response = new DefaultFullHttpResponse(io.netty.handler.codec.http.HttpVersion.HTTP_1_1, status);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        req.release();
    }

    /**
     * 发送 WebSocket 错误帧
     */
    private void sendHttpRequestError(ChannelHandlerContext ctx, FullHttpRequest req, String message) {
        if (log.isDebugEnabled()) {
            log.debug("Sending WebSocket error frame to client: {} with message: {}", ctx.channel().remoteAddress(), message);
        }
        FullHttpResponse response = new DefaultFullHttpResponse(io.netty.handler.codec.http.HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
        response.content().writeBytes(message.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
            if (log.isDebugEnabled()) {
                log.debug("Channel inactive, removed session: {}", sessionId);
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
            log.error("Exception caught, removed session: {}", sessionId, cause);
        } else {
            log.error("Exception caught in handshake handler", cause);
        }
        ctx.close();
    }
}
