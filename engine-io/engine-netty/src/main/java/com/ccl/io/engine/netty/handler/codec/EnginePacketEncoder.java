package com.ccl.io.engine.netty.handler.codec;

import com.ccl.io.engine.codec.EngineIOEncoder;
import com.ccl.io.engine.netty.handler.CorsUtil;
import com.ccl.io.engine.protocol.EngineIOPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
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
 * @since 4.0.0
 */
@Sharable
public class EnginePacketEncoder extends MessageToMessageEncoder<EngineIOPacket<?>> {

    private static final Logger log = LoggerFactory.getLogger(EnginePacketEncoder.class);

    private final EngineIOEncoder encoder;

    public EnginePacketEncoder(EngineIOEncoder encoder) {
        this.encoder = encoder;
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) throws Exception {
        EngineIOPacket.Type type = msg.getType();

        // TODO: 2026/05/10 19:18 websocket binary
        byte[] bytes = encoder.encodePacket(msg, false);
        ByteBuf content = ctx.alloc().buffer().writeBytes(bytes);

        if (log.isDebugEnabled()) {
            log.debug("OUT [{}] {}", type, content.toString(CharsetUtil.UTF_8));
        }

        switch (type) {
            case OPEN:
            case MESSAGE:
                sendResponse(content, out);
                break;
            case CLOSE:
            case PING:
            case PONG:
            case UPGRADE:
            case NOOP:
            default:
                if (log.isWarnEnabled()) {
                    log.warn("未处理的类型 {}", type);
                }
        }
    }

    private void sendResponse(ByteBuf content, List<Object> out) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        CorsUtil.addCorsHeaders(response, "*");
        out.add(response);
    }

}
