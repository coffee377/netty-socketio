package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.core.entity.OpenData;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.core.session.SessionManager;

import io.netty.channel.ChannelInboundHandlerAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
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
public class EngineIOHandshakeHandler extends ChannelInboundHandlerAdapter {

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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());

            String path = queryDecoder.path();
            if (!path.startsWith(connectPath)) {
                if (log.isWarnEnabled()) {
                    log.warn("rejecting invalid path request: {} from client: {}", path, ctx.channel().remoteAddress());
                }
                sendHttpResponse(ctx, request, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            Map<String, List<String>> parameters = queryDecoder.parameters();

            List<String> sids = parameters.get("sid");
            if (sids != null && !sids.isEmpty() && sessionManager.hasSession(sids.get(0))) {
                ClientContext session = sessionManager.getSession(sids.get(0));
                session.setTransportType(TransportType.POLLING);
                ctx.channel().attr(ChannelAttributes.SESSION_ID).set(session.getSessionId());
                ctx.fireChannelRead(request);
                return;
            }

            String transport = parameters.get("transport").stream().findFirst().orElse(null);
            handleHandshake(ctx, request, transport, parameters);
            return;
        }

        super.channelRead(ctx, msg);
    }

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

        boolean isBinary = false;

        if (isBinary) {
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
            response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        }

        addCorsHeaders(response);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void addWebSocketProtocolHandler(ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("Adding WebSocketServerProtocolHandler to pipeline for: {}", ctx.channel().remoteAddress());
        }
        ctx.pipeline().addBefore(ctx.name(), "wsProtocol",
                new WebSocketServerProtocolHandler(null, true, maxFramePayloadLength));
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status) {
        if (log.isDebugEnabled()) {
            log.debug("Sending HTTP error response: {} to client: {}", status, ctx.channel().remoteAddress());
        }
        FullHttpResponse response = new DefaultFullHttpResponse(io.netty.handler.codec.http.HttpVersion.HTTP_1_1, status);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        req.release();
    }

    private void sendHttpRequestError(ChannelHandlerContext ctx, FullHttpRequest req, String message) {
        if (log.isDebugEnabled()) {
            log.debug("Sending WebSocket error frame to client: {} with message: {}", ctx.channel().remoteAddress(), message);
        }
        FullHttpResponse response = new DefaultFullHttpResponse(io.netty.handler.codec.http.HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
        response.content().writeBytes(message.getBytes(StandardCharsets.UTF_8));
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

}
