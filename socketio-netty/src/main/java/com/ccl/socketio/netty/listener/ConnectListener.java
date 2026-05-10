package com.ccl.socketio.netty.listener;

import com.ccl.socketio.core.namespace.Namespace;

/**
 * Socket.IO 连接事件监听器
 *
 * <p>当客户端成功连接时回调此接口。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface ConnectListener {

    /**
     * 客户端连接回调
     *
     * @param client 已连接的 Socket.IO 客户端
     */
    void onConnect(Namespace.SocketIOClient client);

}
