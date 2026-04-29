package com.ccl.socketio.netty.handler;

import com.ccl.socketio.core.namespace.Namespace;
import com.ccl.socketio.core.namespace.NamespaceManager;
import com.ccl.socketio.core.protocol.SocketPacketType;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.channel.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket.IO 命名空间处理器
 *
 * <p>处理 Socket.IO 协议消息，根据包类型分发至对应的处理方法：
 * <ul>
 *   <li>CONNECT：客户端连接命名空间，创建 SocketIOClient 并触发 connect 事件</li>
 *   <li>DISCONNECT：客户端断开连接，触发 disconnect 事件</li>
 *   <li>EVENT：业务事件，触发命名空间上注册的事件监听器</li>
 *   <li>ACK：消息确认，触发对应的回调</li>
 * </ul>
 */
public class SocketIONamespaceHandler extends SimpleChannelInboundHandler<SocketPacket> {

    private final NamespaceManager namespaceManager;
    private final Map<String, Namespace.SocketIOClient> clients = new ConcurrentHashMap<>();

    public SocketIONamespaceHandler(NamespaceManager namespaceManager) {
        this.namespaceManager = namespaceManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocketPacket packet) throws Exception {
        String namespaceName = packet.getNamespace();
        if (namespaceName == null || namespaceName.isEmpty()) {
            namespaceName = "/";
        }

        String sessionId = ctx.channel().attr(com.ccl.engineio.netty.handler.ChannelAttributes.SESSION_ID).get();
        if (sessionId == null) {
            return;
        }

        Namespace namespace = namespaceManager.getNamespace(namespaceName);
        if (namespace == null) {
            return;
        }

        switch (packet.getType()) {
            case CONNECT:
                handleConnect(ctx, namespace, sessionId);
                break;
            case DISCONNECT:
                handleDisconnect(ctx, namespace, sessionId);
                break;
            case EVENT:
                handleEvent(ctx, namespace, sessionId, packet);
                break;
            case ACK:
                handleAck(ctx, namespace, sessionId, packet);
                break;
            default:
                break;
        }
    }

    private void handleConnect(ChannelHandlerContext ctx, Namespace namespace, String sessionId) {
        Namespace.SocketIOClient client = new Namespace.SocketIOClient(sessionId, namespace.getName());
        clients.put(sessionId, client);
        namespace.emit("connect", client);

        SocketPacket ackPacket = new SocketPacket(SocketPacketType.CONNECT, namespace.getName());
        ctx.writeAndFlush(ackPacket);
    }

    private void handleDisconnect(ChannelHandlerContext ctx, Namespace namespace, String sessionId) {
        Namespace.SocketIOClient client = clients.remove(sessionId);
        if (client != null) {
            namespace.emit("disconnect", client);
        }
    }

    private void handleEvent(ChannelHandlerContext ctx, Namespace namespace, String sessionId, SocketPacket packet) {
        Namespace.SocketIOClient client = clients.get(sessionId);
        if (client != null) {
            namespace.emit(packet.getEventName(), client, packet.getData() != null ? packet.getData().toArray() : new Object[0]);
        }
    }

    private void handleAck(ChannelHandlerContext ctx, Namespace namespace, String sessionId, SocketPacket packet) {
        namespace.triggerAck(packet.getAckId(), packet.getData());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().attr(com.ccl.engineio.netty.handler.ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            Namespace.SocketIOClient client = clients.remove(sessionId);
            if (client != null) {
                Namespace namespace = namespaceManager.getNamespace(client.getNamespace());
                if (namespace != null) {
                    namespace.emit("disconnect", client);
                }
            }
        }
        super.channelInactive(ctx);
    }
}
