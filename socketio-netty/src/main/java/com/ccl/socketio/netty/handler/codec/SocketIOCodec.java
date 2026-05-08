package com.ccl.socketio.netty.handler.codec;

import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.engineio.netty.transport.PollingTransport;
import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.codec.impl.SocketIOEncoder;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.channel.ChannelHandler;
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
        if (log.isDebugEnabled()) {
            log.debug("OUT[{}] {}", msg.getType(), msg);
        }
        if (SocketPacket.Type.CONNECT.equals(msg.getType())) {
            ChannelHandlerContext context = ctx.pipeline().context(PollingTransport.class);
            if (context != null) {
                PollingTransport polling = (PollingTransport) context.handler();
                log.error("{}", context);
                String data = encoder.encode(msg);
                EngineIOPacket<String> packet = EngineIOPacket.builder().data(data).build();
                log.warn("{}", packet);
                String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
                polling.sendMessage(sid, packet);
            }
//        out.add(packet);
        }

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, SocketPacket<?> msg, List<Object> out) throws Exception {
        SocketPacket.Type type = msg.getType();
        if (log.isDebugEnabled()) {
            log.debug("IN[{}] {}", type, msg);
        }

        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();

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
