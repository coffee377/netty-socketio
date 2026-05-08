package com.ccl.socketio.netty.handler.codec;

import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.codec.impl.SocketIOEncoder;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

@Sharable
public class SocketIOCodec extends MessageToMessageCodec<SocketPacket<?>, SocketPacket<?>> {
    private final static Logger log = LoggerFactory.getLogger(SocketIOCodec.class);

    private final SocketEncoder encoder;

    public SocketIOCodec() {
        this.encoder = new SocketIOEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SocketPacket<?> msg, List<Object> out) throws Exception {
        log.debug("out: {}", msg);
//        EngineIOPacket<?> engineIOPacket =
//        socketEncoder
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, SocketPacket<?> msg, List<Object> out) throws Exception {
        SocketPacket.Type type = msg.getType();
        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (log.isDebugEnabled()) {
            log.debug("Received {} packet from session {}", type, sid);
        }

        if (type == SocketPacket.Type.CONNECT) {
            HashMap<String, Object> data = new HashMap<String, Object>() {{
                put("sid", sid);
            }};
            SocketPacket<HashMap<String, Object>> packet = SocketPacket.builder()
                    .type(SocketPacket.Type.CONNECT)
                    .data(data).build();
            ctx.channel().writeAndFlush(packet);
            return;
        }
    }
}
