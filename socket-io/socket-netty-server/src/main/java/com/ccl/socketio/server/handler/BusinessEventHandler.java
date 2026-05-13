package com.ccl.socketio.server.handler;

import com.ccl.io.engine.netty.handler.ChannelAttributes;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.server.listener.SocketIOListener;
import io.netty.channel.*;

/**
 * Socket.IO 业务事件处理器
 *
 * <p>接收解码后的 SocketPacket 并委托给 {@link SocketIOListener} 处理业务逻辑。
 * 支持连接、断开、事件和错误四种回调类型。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
@ChannelHandler.Sharable
public class BusinessEventHandler extends SimpleChannelInboundHandler<SocketPacket> {

    private final SocketIOListener listener;

    /**
     * 创建业务事件处理器
     *
     * @param listener SocketIO 事件监听器
     */
    public BusinessEventHandler(SocketIOListener listener) {
        this.listener = listener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocketPacket packet) throws Exception {
        if (listener == null) {
            return;
        }

        String sessionId = ctx.channel().attr(
                ChannelAttributes.SESSION_ID).get();
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
//                    listener.onEvent(
//                            sessionId, namespace, packet.getEventName(),
//                            packet.getData() != null ? packet.getData().toArray() : new Object[0]);
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
