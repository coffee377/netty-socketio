package com.ccl.socketio.netty.handler.codec;

import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.codec.impl.SocketIODecoderV5;
import com.ccl.socketio.core.codec.impl.SocketIOEncoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.core.protocol.SocketPacketType;
import com.ccl.socketio.core.protocol.data.Event;
import com.ccl.socketio.netty.handler.SocketIOChannelAttributes;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
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
        SocketPacket<?> inPacket;
        if (data instanceof String) {
            inPacket = decoder.decode((String) data);
            if (inPacket == null) return;
            if (inPacket.hasAttachments() && !inPacket.isAttachmentsLoaded()) {
                ctx.channel().attr(SocketIOChannelAttributes.SOCKET_PACKET).set(inPacket);
                return;
            }
        } else if (data instanceof byte[]) {
            inPacket = ctx.channel().attr(SocketIOChannelAttributes.SOCKET_PACKET).get();
            if (inPacket != null) {
                inPacket.addAttachment((byte[]) data);
                if (!inPacket.isAttachmentsLoaded()) {
                    return;
                }
            } else {
                return;
            }
        } else {
            return;
        }

        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        SocketPacket.Type type = inPacket.getType();

        if (log.isTraceEnabled()) {
            String packetStr = encoder.encode(inPacket);
            log.trace("IN [{}] {}", inPacket.getType(), packetStr);
        }

        SocketPacket<?> outPacket = null;

        switch (type) {
            case CONNECT:
            case DISCONNECT:
            case ERROR:

            case EVENT:
            case BINARY_EVENT:

        }

//        outPacket = ddd(inPacket, sid);

        if (SocketPacket.Type.CONNECT.equals(type)) {
            HashMap<String, Object> cdata = new HashMap<String, Object>() {{
                put("sid", sid);
            }};
            SocketPacket<HashMap<String, Object>> packet = SocketPacket.builder()
                    .type(SocketPacket.Type.CONNECT)
                    .namespace(inPacket.getNamespace())
                    .data(cdata).build();
            ctx.writeAndFlush(packet);
            return;
        } else if (SocketPacket.Type.DISCONNECT.equals(type)) {
            ctx.close();
            return;
        }

        if (SocketPacket.Type.BINARY_EVENT.equals(type)) {
            byte[] bytes = String.format("data: %s", Instant.now().toString()).getBytes(StandardCharsets.UTF_8);

            Event event = new Event();
            event.setName("pong");
            event.setArgs(Collections.singletonList(bytes));
            // 51-["pong",{"_placeholder":true,"num":0}]
            // 451-["pong",{"_placeholder":true,"num":0}]bZGF0YQ==
            SocketPacket<?> eventPacket = SocketPacket.builder()
                    .type(SocketPacket.Type.BINARY_EVENT)
                    .namespace(inPacket.getNamespace())
                    .attachmentsCount(1)
                    .data(event).build();
            eventPacket.addAttachment(bytes);
            ctx.channel().writeAndFlush(eventPacket);
        }

        if (outPacket != null) {
            ctx.channel().writeAndFlush(outPacket);
        }
    }
}
