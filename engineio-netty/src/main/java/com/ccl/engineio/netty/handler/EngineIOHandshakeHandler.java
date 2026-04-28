package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.entity.OpenData;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.core.session.SessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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

    public EngineIOHandshakeHandler(String connectPath, int maxFramePayloadLength) {
        this.connectPath = connectPath;
        this.sessionManager = SessionManager.getInstance();
        this.maxFramePayloadLength = maxFramePayloadLength;
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

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(fullHttpRequest.uri());
        String path = queryStringDecoder.path();

        if (!path.startsWith(connectPath)) {
            if (log.isDebugEnabled()) {
                log.debug("rejecting invalid path request: {} from client: {}", path, ctx.channel().remoteAddress());
            }
            sendHttpResponse(ctx, fullHttpRequest, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        Map<String, List<String>> params = queryStringDecoder.parameters();
        List<String> sidValues = params.get("sid");
        List<String> transportValues = params.get("transport");

        if (sidValues == null || sidValues.isEmpty()) {
            if (!handleHandshake(ctx, fullHttpRequest, params, transportValues)) {
                fullHttpRequest.release();
                return;
            }
        } else {
            ctx.channel().attr(ChannelAttributes.SESSION_ID).set(sidValues.get(0));
        }

        ctx.channel().attr(CONNECTED_HTTP_REQUEST).set(fullHttpRequest);
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
        openData.setPingInterval((int) sessionManager.getPingInterval());
        openData.setPingTimeout((int) sessionManager.getPingTimeout());
        openData.setMaxPayload(maxFramePayloadLength);

        EngineIOPacket<String> packet = EngineIOPacket.of(EngineIOPacket.Type.OPEN);
        try {
            String json = OBJECT_MAPPER.writeValueAsString(openData);
            packet = EngineIOPacket.of(EngineIOPacket.Type.OPEN, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OpenData", e);
            sendHttpResponse(ctx, fullHttpRequest, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return false;
        }

        String encodedPacket = ByteBufUtil.hexDump(Unpooled.wrappedBuffer(
                packet.getType().getByte(),
                packet.getData().getBytes(StandardCharsets.UTF_8)));
        byte[] packetBytes = new byte[1];
        packetBytes[0] = packet.getType().getByte();
        byte[] payloadBytes = packet.getData().getBytes(StandardCharsets.UTF_8);
        byte[] responseBytes = new byte[packetBytes.length + payloadBytes.length];
        System.arraycopy(packetBytes, 0, responseBytes, 0, packetBytes.length);
        System.arraycopy(payloadBytes, 0, responseBytes, packetBytes.length, payloadBytes.length);
        FullHttpResponse response = new DefaultFullHttpResponse(
                io.netty.handler.codec.http.HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(responseBytes));
        response.headers().set(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        return true;
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
