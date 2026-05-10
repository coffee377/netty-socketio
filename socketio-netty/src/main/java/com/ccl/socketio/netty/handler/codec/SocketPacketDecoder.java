package com.ccl.socketio.netty.handler.codec;

import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.codec.impl.SocketIODecoderV5;
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
public class SocketIODecoder extends MessageToMessageDecoder<EngineIOPacket<?>> {

    private static final Logger log = LoggerFactory.getLogger(SocketIODecoder.class);

    private final SocketDecoder decoder;

    public SocketIODecoder() {
        this.decoder = new SocketIODecoderV5();
    }

    public SocketIODecoder(SocketDecoder decoder) {
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
        SocketPacket<?> socketPacket;
        if (data instanceof String) {
            socketPacket = decoder.decode((String) data);
            if (socketPacket == null) return;
            if (socketPacket.hasAttachments() && !socketPacket.isAttachmentsLoaded()) {
                ctx.channel().attr(SocketIOChannelAttributes.SOCKET_PACKET).set(socketPacket);
                return;
            }
        } else if (data instanceof byte[]) {
            socketPacket = ctx.channel().attr(SocketIOChannelAttributes.SOCKET_PACKET).get();
            if (socketPacket != null) {
                socketPacket.addAttachment((byte[]) data);
                if (!socketPacket.isAttachmentsLoaded()) {
                    return;
                }
            } else {
                return;
            }
        } else {
            return;
        }

        if (log.isDebugEnabled()) {
            String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
            log.debug("IN[{}] {} sid={}", socketPacket.getType(), socketPacket.getData(), sid);
        }

        out.add(socketPacket);
    }
}
