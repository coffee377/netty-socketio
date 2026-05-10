package com.ccl.socketio.core.listener;

import com.ccl.socketio.core.namespace.SocketIOClient;

/**
 * Socket.IO 断开连接事件监听器
 *
 * <p>当客户端断开连接时回调此接口。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface DisconnectListener {

    /**
     * 客户端断开连接回调
     *
     * @param client 断开连接的客户端
     */
    void onDisconnect(SocketIOClient client);

}
