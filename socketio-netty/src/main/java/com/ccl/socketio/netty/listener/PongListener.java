package com.ccl.socketio.netty.listener;

import com.ccl.socketio.core.namespace.Namespace;

/**
 * Socket.IO Pong 响应监听器
 *
 * <p>当收到客户端的 Pong 心跳响应时回调此接口。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface PongListener {

    /**
     * Pong 响应回调
     *
     * @param client 发送 Pong 响应的客户端
     */
    void onPong(Namespace.SocketIOClient client);

}
