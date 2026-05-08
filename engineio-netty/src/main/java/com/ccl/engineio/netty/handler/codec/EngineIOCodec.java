package com.ccl.engineio.netty.handler.codec;

import com.ccl.engineio.core.codec.Encoder;
import com.ccl.engineio.core.codec.impl.EngineV4Encoder;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.codec.impl.SocketIODecoder;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Sharable
public class EngineIOCodec extends MessageToMessageCodec<EngineIOPacket<?>, EngineIOPacket<?>> {
    private static final Logger log = LoggerFactory.getLogger(EngineIOCodec.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final boolean enableCors;
    private final String corsOrigin;

    private final SocketDecoder decoder;
    private final Encoder encoder;


    public EngineIOCodec() {
        this("*", true);
    }

    public EngineIOCodec(String corsOrigin, boolean enableCors) {
        this.corsOrigin = corsOrigin;
        this.enableCors = enableCors;
        this.decoder = new SocketIODecoder();
        this.encoder = new EngineV4Encoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) throws Exception {
        EngineIOPacket.Type type = msg.getType();
        if (log.isDebugEnabled()) {
            log.debug("OUT[{}] {}", type, msg);
        }
        switch (type) {
            case OPEN:
                sendOpenData(ctx, msg, out);
                return;
            case CLOSE:
            case PING:
            case PONG:
            case MESSAGE:
                sendMessage(ctx, msg, out);
                break;
            case UPGRADE:
            case NOOP:
        }
    }

    private void sendMessage(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        addCorsHeaders(response);
        // websocket binary
        byte[] bytes = encoder.encodePacket(msg, false);
        response.content().writeBytes(bytes);
        out.add(response);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) throws Exception {
        EngineIOPacket.Type type = msg.getType();
        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (log.isDebugEnabled()) {
            log.debug("IN[{}] {}", type, msg);
        }
        switch (type) {
            case OPEN:
                break;
            case CLOSE:
                ctx.close();
                break;
            case PING:
            case PONG:
            case MESSAGE:
                Object data = msg.getData();
                String raw = null;
                if (data instanceof String) {
                    raw = (String) data;
                } else if (data instanceof ByteBuf) {
                    raw = ((ByteBuf) data).toString(CharsetUtil.UTF_8);
                } else if (data instanceof byte[]) {
                    raw = new String((byte[]) data, StandardCharsets.UTF_8);
                }
                SocketPacket<?> socketPacket = decoder.decode(raw);
                if (socketPacket != null && socketPacket.isAttachmentsLoaded()) {
                    out.add(socketPacket);
                }
                break;
            case UPGRADE:
            case NOOP:
        }
    }

    private void sendOpenData(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) {
        byte[] bytes = encoder.encodePacket(msg, false);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        addCorsHeaders(response);
        out.add(response);
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

}
