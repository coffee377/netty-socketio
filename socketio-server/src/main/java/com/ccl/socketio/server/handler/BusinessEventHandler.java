package com.ccl.socketio.server.handler;

import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.server.listener.SocketIOListener;
import io.netty.channel.*;

@ChannelHandler.Sharable
public class BusinessEventHandler extends SimpleChannelInboundHandler<SocketPacket> {

    private final SocketIOListener listener;

    public BusinessEventHandler(SocketIOListener listener) {
        this.listener = listener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocketPacket packet) throws Exception {
        if (listener == null) {
            return;
        }

        String sessionId = ctx.channel().attr(
                com.ccl.engineio.netty.handler.ChannelAttributes.SESSION_ID).get();
        String namespace = packet.getNamespace();
        if (namespace == null || namespace.isEmpty()) {
            namespace = "/";
        }

        try {
            switch (packet.getType()) {
                case CONNECT:
                    listener.onConnect(sessionId, namespace);
                    break;
                case DISCONNECT:
                    listener.onDisconnect(sessionId, namespace);
                    break;
                case EVENT:
                    listener.onEvent(
                            sessionId, namespace, packet.getEventName(),
                            packet.getData() != null ? packet.getData().toArray() : new Object[0]);
                    break;
                case ACK:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            listener.onError(sessionId, namespace, e);
        }
    }
}
