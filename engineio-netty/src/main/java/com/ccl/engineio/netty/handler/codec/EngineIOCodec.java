package com.ccl.engineio.netty.handler.codec;

import com.ccl.engineio.core.codec.EngineIOEncoder;
import com.ccl.engineio.core.codec.impl.EngineIOEncoderV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.exception.EngineIOException;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.codec.impl.SocketIODecoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Engine.IO 编解码处理器
 *
 * <p>继承 Netty 的 MessageToMessageCodec，实现 Engine.IO 数据包的编解码：
 * <ul>
 *   <li>解码：将 EngineIOPacket 转换为 SocketPacket（处理二进制附件组装）</li>
 *   <li>编码：将 EngineIOPacket 编码为 HTTP 响应数据</li>
 * </ul>
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 * @see com.ccl.engineio.core.codec.EngineIOEncoder
 * @see com.ccl.engineio.core.codec.EngineIODecoder
 */
@Sharable
public class EngineIOCodec extends MessageToMessageCodec<EngineIOPacket<?>, EngineIOPacket<?>> {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(EngineIOCodec.class);

    /**
     * 是否启用 CORS
     */
    private final boolean enableCors;

    /**
     * CORS 允许的来源
     */
    private final String corsOrigin;

    /**
     * Socket.IO 解码器
     */
    private final SocketDecoder decoder;

    /**
     * Engine.IO 编码器
     */
    private final EngineIOEncoder encoder;

    /**
     * 默认构造函数
     */
    public EngineIOCodec() {
        this("*", true);
    }

    /**
     * 构造函数
     *
     * @param corsOrigin  CORS 允许的来源，"*" 表示允许所有
     * @param enableCors  是否启用 CORS
     */
    public EngineIOCodec(String corsOrigin, boolean enableCors) {
        this.corsOrigin = corsOrigin;
        this.enableCors = enableCors;
        this.decoder = new SocketIODecoderV5();
        this.encoder = new EngineIOEncoderV4();
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
                SocketPacket<?> socketPacket;
                if (data instanceof String) {
                    socketPacket = decoder.decode((String) data);
                    if (socketPacket.hasAttachments() && !socketPacket.isAttachmentsLoaded()) {
                        ctx.channel().attr(ChannelAttributes.SOCKET_PACKET).set(socketPacket);
                    }
                } else if (data instanceof byte[]) {
                    socketPacket = ctx.channel().attr(ChannelAttributes.SOCKET_PACKET).get();
                    if (socketPacket != null) {
                        socketPacket.addAttachment((byte[]) data);
                    }
                } else {
                    throw new EngineIOException("类型错误");
                }
                if (socketPacket != null && socketPacket.isAttachmentsLoaded()) {
                    out.add(socketPacket);
                }
                return;
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
