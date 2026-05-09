package com.ccl.socketio.core.event;

import com.ccl.socketio.core.namespace.Namespace;
import com.ccl.socketio.core.protocol.data.Event;

/**
 * Socket.IO 事件处理器接口
 *
 * <p>定义 Socket.IO 客户端的事件回调方法，
 * 用户实现此接口来处理连接、断开、事件和错误</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 * @see Namespace.SocketIOClient
 * @see Event
 */
public interface EventHandler {

    /**
     * 连接建立时回调
     *
     * @param client 已连接的客户端
     */
    void onConnect(Namespace.SocketIOClient client);

    /**
     * 断开连接时回调
     *
     * @param client 断开连接的客户端
     */
    void onDisconnect(Namespace.SocketIOClient client);

    /**
     * 收到事件时回调
     *
     * @param client 已连接客户端
     * @param event  事件数据
     */
    void onEvent(Namespace.SocketIOClient client, Event event);

    /**
     * 发生错误时回调
     *
     * @param client 出错的客户端
     * @param error  异常信息
     */
    void onError(Namespace.SocketIOClient client, Throwable error);
}
