package com.ccl.socketio.netty.handler.codec;

import com.ccl.io.engine.codec.EngineIOEncoder;
import com.ccl.io.engine.core.codec.impl.EngineIOEncoderV4;
import com.ccl.io.engine.netty.handler.ChannelAttributes;
import com.ccl.io.engine.netty.transport.PollingTransport;
import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.codec.impl.SocketIOEncoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Socket.IO 出站数据包编码处理器
 *
 * <p>将 {@link SocketPacket} 编码为 Engine.IO 协议层的数据包，
 * 并通过 {@link PollingTransport} 发送到客户端。
 * 支持带二进制附件的消息拆分编码。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
@Sharable
public class SocketPacketEncoder extends MessageToMessageEncoder<SocketPacket<?>> {
    private static final Logger log = LoggerFactory.getLogger(SocketPacketEncoder.class);

    private final SocketEncoder encoder;
    private final EngineIOEncoder engineEncoder;

    public SocketPacketEncoder(SocketEncoder encoder) {
        this.encoder = encoder;
        this.engineEncoder = new EngineIOEncoderV4();
    }

    public SocketPacketEncoder() {
        this(new SocketIOEncoderV5());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SocketPacket<?> msg, List<Object> out) throws Exception {
        String ps = encoder.encode(msg);
        if (log.isDebugEnabled()) {
            log.debug("OUT [{}] {}", msg.getType(), ps);
        }

        List<EngineIOPacket<?>> packets = new ArrayList<>();
        EngineIOPacket<String> packet = EngineIOPacket.builder().data(ps).build();
        packets.add(packet);

        if (msg.hasAttachments()) {
            for (byte[] attachment : msg.getAttachments()) {
                EngineIOPacket<byte[]> ap = EngineIOPacket.builder().data(attachment).build();
                packets.add(ap);
            }
        }

        // TODO: 2026/05/10 19:06 根据 transport 选择信息处理方式

        ChannelHandlerContext context = ctx.pipeline().context(PollingTransport.class);
        if (context != null) {
            ByteBuffer byteBuffer = engineEncoder.encodePayload(packets, false);
            int remaining = byteBuffer.remaining();
            byte[] bytes = new byte[remaining];
            byteBuffer.get(bytes);

            ByteBuf content = ctx.alloc().buffer();
            content.writeBytes(bytes);

            PollingTransport polling = (PollingTransport) context.handler();
            String sid = context.channel().attr(ChannelAttributes.SESSION_ID).get();
            polling.sendMessage(sid, content);
        }

    }

}
