package com.ccl.socketio.netty.handler;

import com.ccl.socketio.core.event.EventRouter;
import com.ccl.socketio.core.namespace.NamespaceManager;
import com.ccl.socketio.core.namespace.SocketIONamespace;
import com.ccl.socketio.core.namespace.impl.Namespace;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.core.protocol.data.Event;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Socket.IO 事件路由处理器
 *
 * <p>接收解码后的 SocketPacket 并路由到对应的事件处理器。
 * 当前包含临时测试逻辑，用于验证二进制事件的处理流程。
 *
 * @author coffee377
 * @since 4.0.0
 */
public class SocketIOEventRouterHandler extends SimpleChannelInboundHandler<SocketPacket<?>> {
    private final static Logger log = LoggerFactory.getLogger(SocketIOEventRouterHandler.class);
    private final EventRouter eventRouter;
    private final NamespaceManager namespaceManager;

    /**
     * 创建事件路由处理器
     *
     * @param eventRouter 事件路由器
     */
    public SocketIOEventRouterHandler(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
        this.namespaceManager = new NamespaceManager();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocketPacket<?> packet) throws Exception {
        SocketIONamespace namespace = namespaceManager.getNamespace(packet.getNamespace());

        log.warn("{}", packet);

        // TODO: 2026/05/10 22:55 临时测试
        if (packet.getType() == SocketPacket.Type.BINARY_EVENT) {
            byte[] b1 = "hello".getBytes(StandardCharsets.UTF_8);
            byte[] b2 = "world".getBytes(StandardCharsets.UTF_8);
            byte[] b3 = Instant.now().toString().getBytes(StandardCharsets.UTF_8);

            Event event = new Event();
            event.setName("pong");
            event.setArgs(Arrays.asList(b1, b2, b3));
            SocketPacket<Event> out = SocketPacket.builder().type(SocketPacket.Type.BINARY_EVENT)
                    .attachmentsCount(3)
                    .namespace(packet.getNamespace())
                    .data(event).build();

            out.addAttachment(b1);
            out.addAttachment(b2);
            out.addAttachment(b3);

            ctx.writeAndFlush(out);
        }

//        namespace.getClient("").send();
//        if (eventName != null) {
//            eventRouter.route(namespace, eventName, null, packet.getData());
//        }
    }

    /**
     * 注册事件处理器
     *
     * @param namespace 命名空间名称
     * @param eventName 事件名称
     * @param handler   事件处理回调
     */
    public void registerEventHandler(String namespace, String eventName, Consumer<Namespace.SocketClient> handler) {
        eventRouter.registerHandler(namespace, eventName, handler);
    }

    /**
     * 注销事件处理器
     *
     * @param namespace 命名空间名称
     * @param eventName 事件名称
     */
    public void unregisterEventHandler(String namespace, String eventName) {
        eventRouter.unregisterHandler(namespace, eventName);
    }
}
