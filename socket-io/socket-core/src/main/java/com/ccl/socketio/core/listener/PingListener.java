package com.ccl.socketio.core.listener;

import com.ccl.socketio.core.namespace.SocketIOClient;

/**
 * Socket.IO Ping 心跳监听器
 *
 * <p>当服务端向客户端发送 Ping 心跳时回调此接口。
 *
 * @author coffee377
 * @since 4.0.0
 */
public interface PingListener {

    /**
     * Ping 心跳发送回调
     *
     * @param client 接收 Ping 的客户端
     */
    void onPing(SocketIOClient client);

}
