package com.ccl.engineio.netty.handler.codec;

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


    public EngineIOCodec() {
        this("*", true);
    }

    public EngineIOCodec(String corsOrigin, boolean enableCors) {
        this.corsOrigin = corsOrigin;
        this.enableCors = enableCors;
        this.decoder = new SocketIODecoder();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) throws Exception {
        EngineIOPacket.Type type = msg.getType();
        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (log.isDebugEnabled()) {
            log.debug("Received {} packet from session {}", type, sid);
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
                if (log.isDebugEnabled()) {
                    log.debug("Received data: {}", raw);
                }
                SocketPacket<?> socketPacket = decoder.decode(raw);
                if (socketPacket != null) {
                    out.add(socketPacket);
                }
                break;
            case UPGRADE:
            case NOOP:
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) throws Exception {
        EngineIOPacket.Type type = msg.getType();
        if (log.isDebugEnabled()) {
            log.debug("EngineIOCodec encode msg type:{}, msg:{}", type, msg);
        }
        switch (type) {
            case OPEN:
                log.debug(EngineIOPacket.Type.OPEN.getDescription());
                sendOpenData(ctx, msg, out);
                return;
            case CLOSE:
                log.debug(EngineIOPacket.Type.CLOSE.getDescription());
                break;
            case PING:
                log.debug(EngineIOPacket.Type.PING.getDescription());
                break;
            case PONG:
                log.debug(EngineIOPacket.Type.PONG.getDescription());
                break;
            case MESSAGE:
                log.debug(EngineIOPacket.Type.MESSAGE.getDescription());
                break;
            case UPGRADE:
                log.debug(EngineIOPacket.Type.UPGRADE.getDescription());
                break;
            case NOOP:
                log.debug(EngineIOPacket.Type.NOOP.getDescription());
                break;
        }
    }

    private void sendOpenData(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) {
        EngineIOPacket<String> packet;
        try {
            String json = OBJECT_MAPPER.writeValueAsString(msg.getData());
            packet = EngineIOPacket.builder().type(EngineIOPacket.Type.OPEN).data(json).build();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OpenData", e);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
//            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            out.add(response);
            return;
        }

        byte[] bytes = ParserV4.getInstance().encodePacket(packet, true);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        if (log.isDebugEnabled()) {
            log.debug("{}", byteBuf.toString(StandardCharsets.UTF_8));
        }
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
