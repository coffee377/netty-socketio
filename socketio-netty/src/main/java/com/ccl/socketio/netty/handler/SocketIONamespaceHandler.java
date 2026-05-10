package com.ccl.socketio.netty.handler;

import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.socketio.core.namespace.SocketIOClient;
import com.ccl.socketio.core.namespace.SocketIONamespace;
import com.ccl.socketio.core.namespace.impl.Namespace;
import com.ccl.socketio.core.namespace.NamespaceManager;
import com.ccl.socketio.core.namespace.impl.NamespaceClient;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket.IO 命名空间处理器
 *
 * <p>处理 Socket.IO 协议消息，根据包类型分发至对应的处理方法：
 * <ul>
 *   <li>CONNECT：客户端连接命名空间，创建 SocketIOClient 并触发 connect 事件</li>
 *   <li>DISCONNECT：客户端断开连接，触发 disconnect 事件</li>
 *   <li>其他类型（EVENT、ACK 等）：透传到下游处理器</li>
 * </ul>
 */
@Sharable
public class SocketIONamespaceHandler extends SimpleChannelInboundHandler<SocketPacket<?>> {

    private final NamespaceManager namespaceManager;

    public SocketIONamespaceHandler(NamespaceManager namespaceManager) {
        this.namespaceManager = namespaceManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocketPacket<?> packet) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId == null) return;

        String namespaceName = packet.getNamespace();
        SocketIONamespace namespace = namespaceManager.getOrCreateNamespace(namespaceName);
        // TODO: 2026/05/10 22:05 获取 SocketIOClient
        // namespace.addClient();

        switch (packet.getType()) {
            case CONNECT:
                handleConnect(ctx, namespace, sessionId);
                break;
            case DISCONNECT:
                handleDisconnect(ctx, namespace, sessionId);
                break;
            default:
                ctx.fireChannelRead(packet);
                break;
        }
    }

    private void handleConnect(ChannelHandlerContext ctx, SocketIONamespace namespace, String sessionId) {
        NamespaceClient client = new NamespaceClient(namespace, sessionId);
        namespace.addClient(client);
//        namespace.emit("connect", client);

        SocketPacket<?> packet = SocketPacket.builder()
                .type(SocketPacket.Type.CONNECT)
                .namespace(namespace.getName())
                .data(new HashMap<String, Object>() {{
                    put("sid", sessionId);
                }})
                .build();
        ctx.writeAndFlush(packet);
    }

    private void handleDisconnect(ChannelHandlerContext ctx, SocketIONamespace namespace, String sessionId) {
        SocketIOClient client = namespace.removeClient(sessionId);
        if (client != null) {
            // namespace.emit("disconnect", client);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sid != null) {
//            namespaceManager.g
//            Namespace.SocketClient client = clients.remove(sessionId);
//            if (client != null) {
//                Namespace namespace = namespaceManager.getNamespace(client.getNamespace());
//                if (namespace != null) {
//                    namespace.emit("disconnect", client);
//                }
//            }
        }
        super.channelInactive(ctx);
    }
}
