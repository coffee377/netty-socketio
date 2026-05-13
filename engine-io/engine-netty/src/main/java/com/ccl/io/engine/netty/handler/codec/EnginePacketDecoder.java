package com.ccl.io.engine.netty.handler.codec;

import com.ccl.io.engine.codec.EngineIODecoder;
import com.ccl.io.engine.core.codec.impl.EngineIODecoderV4;
import com.ccl.io.engine.core.entity.ClientContext;
import com.ccl.io.engine.message.EngineMessage;
import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.Transport;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Engine.IO 入站数据包解码处理器
 *
 * <p>将 {@link EngineMessage} 解码为 {@link EngineIOPacket}，
 * 并处理 CLOSE、PING、PONG 等协议级控制消息。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
@Sharable
public class EnginePacketDecoder extends MessageToMessageDecoder<EngineMessage> {

    private static final Logger log = LoggerFactory.getLogger(EnginePacketDecoder.class);

    private final EngineIODecoder decoder;

    public EnginePacketDecoder(EngineIODecoder decoder) {
        this.decoder = decoder;
    }

    public EnginePacketDecoder() {
        this(new EngineIODecoderV4());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, EngineMessage msg, List<Object> out) throws Exception {
        ByteBuf content = msg.getContent();
        ClientContext client = msg.getClient();

        if (log.isTraceEnabled()) {
            log.trace("IN message: {} for sessionId: {}", content.toString(CharsetUtil.UTF_8), client.getSessionId());
        }

        Transport transport = msg.getTransport();
        if (Transport.WEBSOCKET.equals(transport)) {
            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes);
            List<EngineIOPacket<?>> packets = decoder.decodePayload(bytes);
            for (EngineIOPacket<?> packet : packets) {
                processSinger(packet, out, ctx);
            }
        } else {
            String result = content.readString(content.readableBytes(), CharsetUtil.UTF_8);
            EngineIOPacket<?> packet = decoder.decodePacket(result);
            processSinger(packet, out, ctx);
        }
    }

    private void processSinger(EngineIOPacket<?> packet, List<Object> out, ChannelHandlerContext ctx) {
        EngineIOPacket.Type type = packet.getType();

        if (log.isDebugEnabled()) {
            log.debug("IN [{}] {}", type, packet);
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
                out.add(packet);
                return;
            case UPGRADE:
            case NOOP:
        }
    }
}
