package com.ccl.socketio.netty.handler;

import com.ccl.socketio.core.event.EventRouter;
import com.ccl.socketio.core.namespace.Namespace;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.channel.*;

import java.util.function.Consumer;

public class SocketIOEventRouterHandler extends SimpleChannelInboundHandler<SocketPacket> {

    private final EventRouter eventRouter;

    public SocketIOEventRouterHandler(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocketPacket packet) throws Exception {
        String namespace = packet.getNamespace();
        if (namespace == null || namespace.isEmpty()) {
            namespace = "/";
        }

        String eventName = packet.getEventName();
        if (eventName != null && !eventName.isEmpty()) {
            eventRouter.route(namespace, eventName, null, packet.getData() != null ? packet.getData().toArray() : new Object[0]);
        }
    }

    public void registerEventHandler(String namespace, String eventName, Consumer<Namespace.SocketIOClient> handler) {
        eventRouter.registerHandler(namespace, eventName, handler);
    }

    public void unregisterEventHandler(String namespace, String eventName) {
        eventRouter.unregisterHandler(namespace, eventName);
    }
}
