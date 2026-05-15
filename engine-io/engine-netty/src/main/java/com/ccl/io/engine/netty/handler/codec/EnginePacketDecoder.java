package com.ccl.io.engine.netty.handler.codec;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.codec.EngineIODecoder;
import com.ccl.io.engine.message.EngineMessagePack;
import com.ccl.io.engine.netty.handler.ChannelAttributes;
import com.ccl.io.engine.protocol.EngineIOPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Engine.IO 入站数据包解码处理器
 *
 * <p>将 {@link EngineMessagePack} 解码为 {@link EngineIOPacket}，
 * 并处理 CLOSE、PING、PONG 等协议级控制消息。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
@Sharable
public class EnginePacketDecoder extends MessageToMessageDecoder<EngineMessagePack> {

    private static final Logger log = LoggerFactory.getLogger(EnginePacketDecoder.class);

    private final EngineIODecoder decoder;

    public EnginePacketDecoder(EngineIODecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, EngineMessagePack msg, List<Object> out) throws Exception {
        ByteBuf content = msg.getContent();
        EngineClient client = msg.getClient();
        EngineClient client2 = ctx.channel().attr(ChannelAttributes.ENGINE_CLIENT).get();

        Object eio = ctx.channel().attr(AttributeKey.valueOf("EIO")).setIfAbsent(4);

        if (log.isTraceEnabled()) {
            log.trace("IN message: {} for sessionId: {}", content.toString(CharsetUtil.UTF_8), client.getSessionId());
        }

        ByteArrayOutputStream outs = new ByteArrayOutputStream();
        content.readBytes(outs, content.readableBytes());
        List<EngineIOPacket<?>> packets = decoder.decodePayload(outs.toByteArray());

        for (EngineIOPacket<?> packet : packets) {
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
