package com.ccl.socketio.netty.handler.codec;

import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.codec.impl.SocketIODecoderV5;
import com.ccl.socketio.core.codec.impl.SocketIOEncoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.netty.handler.SocketIOChannelAttributes;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Socket.IO 入站解码处理器
 *
 * <p>将 Engine.IO 协议层的 {@link EngineIOPacket} 解码为 {@link SocketPacket}，
 * 完成二进制附件的重组组装。
 * </p>
 *
 * <p>职责边界：
 * <ul>
 *   <li>仅处理 MESSAGE 类型的 EngineIOPacket</li>
 *   <li>处理二进制附件的分片重组</li>
 *   <li>解码完成后输出完整的 SocketPacket 到下游</li>
 * </ul>
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
@Sharable
public class SocketPacketDecoder extends MessageToMessageDecoder<EngineIOPacket<?>> {

    private static final Logger log = LoggerFactory.getLogger(SocketPacketDecoder.class);

    private final SocketDecoder decoder;
    private final SocketEncoder encoder;

    public SocketPacketDecoder() {
        this.encoder = new SocketIOEncoderV5();
        this.decoder = new SocketIODecoderV5();
    }

    public SocketPacketDecoder(SocketEncoder encoder, SocketDecoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return super.acceptInboundMessage(msg) && isEngine((EngineIOPacket<?>) msg);
    }

    private boolean isEngine(EngineIOPacket<?> msg) {
        Object data = msg.getData();
        if (data instanceof String || data instanceof byte[]) {
            return EngineIOPacket.Type.MESSAGE.equals(msg.getType());
        }
        return false;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, EngineIOPacket<?> msg, List<Object> out) throws Exception {
        Object data = msg.getData();
        SocketPacket<?> packet = decodePacket(data, ctx);
        if (packet == null) return;

        if (log.isTraceEnabled()) {
            log.trace("IN [{}] {}", packet.getType(), encoder.encode(packet));
        }

        out.add(packet);
    }

    private SocketPacket<?> decodePacket(Object data, ChannelHandlerContext ctx) {
        if (data instanceof String) {
            SocketPacket<?> packet = decoder.decode((String) data);
            if (packet == null) return null;
            if (packet.hasAttachments() && !packet.isAttachmentsLoaded()) {
                ctx.channel().attr(SocketIOChannelAttributes.SOCKET_PACKET).set(packet);
                return null;
            }
            return packet;
        }
        if (data instanceof byte[]) {
            SocketPacket<?> packet = ctx.channel().attr(SocketIOChannelAttributes.SOCKET_PACKET).get();
            if (packet != null) {
                packet.addAttachment((byte[]) data);
                if (packet.isAttachmentsLoaded()) {
                    return packet;
                }
            }
            return null;
        }
        return null;
    }
}
