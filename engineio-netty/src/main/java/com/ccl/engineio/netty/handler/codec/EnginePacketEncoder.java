package com.ccl.engineio.netty.handler.codec;

import com.ccl.engineio.core.codec.EngineIOEncoder;
import com.ccl.engineio.core.codec.impl.EngineIOEncoderV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.netty.handler.CorsUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Engine.IO 出站数据包编码处理器
 *
 * <p>将 {@link EngineIOPacket} 编码为 HTTP 响应输出，
 * 根据数据包类型分发到不同的编码路径：
 * <ul>
 *   <li>OPEN 类型编码为含握手数据的 HTTP 200 响应</li>
 *   <li>MESSAGE/PING/PONG/CLOSE 类型编码为普通 HTTP 响应</li>
 * </ul>
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
@Sharable
public class EnginePacketEncoder extends MessageToMessageEncoder<EngineIOPacket<?>> {

    private static final Logger log = LoggerFactory.getLogger(EnginePacketEncoder.class);

    private final EngineIOEncoder encoder;

    public EnginePacketEncoder(EngineIOEncoder encoder) {
        this.encoder = encoder;
    }

    public EnginePacketEncoder() {
        this(new EngineIOEncoderV4());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) throws Exception {
        EngineIOPacket.Type type = msg.getType();

        if (log.isDebugEnabled()) {
            log.debug("OUT [{}] {}", type, msg);
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

    private void sendOpenData(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) {
        byte[] bytes = encoder.encodePacket(msg, false);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        CorsUtil.addCorsHeaders(response, "*");
        out.add(response);
    }

    private void sendMessage(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        CorsUtil.addCorsHeaders(response, "*");
        // websocket binary
        byte[] bytes = encoder.encodePacket(msg, false);
        response.content().writeBytes(bytes);
        out.add(response);
    }

}
