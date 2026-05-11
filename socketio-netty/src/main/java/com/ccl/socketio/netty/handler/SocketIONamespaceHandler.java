package com.ccl.socketio.netty.handler;

import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.socketio.core.namespace.SocketIOClient;
import com.ccl.socketio.core.namespace.SocketIONamespace;
import com.ccl.socketio.core.namespace.NamespaceManager;
import com.ccl.socketio.core.namespace.impl.NamespaceClient;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;

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

    /**
     * 创建命名空间处理器
     *
     * @param namespaceManager 命名空间管理器
     */
    public SocketIONamespaceHandler(NamespaceManager namespaceManager) {
        this.namespaceManager = namespaceManager;
    }

    /**
     * 处理 Socket.IO 协议数据包
     *
     * <p>根据数据包类型分发处理：
     * <ul>
     *   <li>CONNECT — 处理客户端命名空间连接</li>
     *   <li>DISCONNECT — 处理客户端断开连接</li>
     *   <li>其他类型 — 透传到下游 ChannelHandler</li>
     * </ul>
     *
     * @param ctx    Channel 处理器上下文
     * @param packet Socket.IO 数据包
     * @throws Exception 处理过程中的异常
     */
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
            case EVENT:
            case BINARY_EVENT:
            case ACK:
            case BINARY_ACK:
            case ERROR:
            default:
                ctx.fireChannelRead(packet);
                break;
        }
    }

    /**
     * 处理客户端连接命名空间
     *
     * <p>创建客户端实例添加到命名空间，发送 CONNECT 确认包。
     *
     * @param ctx       Channel 处理器上下文
     * @param namespace 目标命名空间
     * @param sessionId 客户端会话 ID
     */
    private void handleConnect(ChannelHandlerContext ctx, SocketIONamespace namespace, String sessionId) {
        NamespaceClient client = new NamespaceClient(namespace, sessionId);
        namespace.emit("connect", client);

        SocketPacket<?> packet = SocketPacket.builder()
                .type(SocketPacket.Type.CONNECT)
                .namespace(namespace.getName())
                .data(new HashMap<String, String>() {{
                    put("sid", sessionId);
                }})
                .build();

        ctx.writeAndFlush(packet);
    }

    /**
     * 处理客户端断开连接
     *
     * <p>从命名空间中移除客户端并触发断开事件。
     *
     * @param ctx       Channel 处理器上下文
     * @param namespace 所属命名空间
     * @param sessionId 客户端会话 ID
     */
    private void handleDisconnect(ChannelHandlerContext ctx, SocketIONamespace namespace, String sessionId) {
        SocketIOClient client = namespace.removeClient(sessionId);
        if (client != null) {
            // namespace.emit("disconnect", client);
        }
    }

    /**
     * Channel 断开时清理客户端会话
     *
     * @param ctx Channel 处理器上下文
     * @throws Exception 清理过程中的异常
     */
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
