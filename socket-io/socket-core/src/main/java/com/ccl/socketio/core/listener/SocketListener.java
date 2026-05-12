package com.ccl.socketio.core.listener;

import com.ccl.socketio.core.namespace.SocketIOClient;
import com.ccl.socketio.core.protocol.data.Event;

/**
 * Socket.IO 客户端事件监听器
 *
 * <p>定义客户端在命名空间下的连接、断开、事件接收和错误处理回调。
 * 继承 {@link ConnectListener} 和 {@link DisconnectListener} 接口。
 *
 * @since 4.0.0-alpha.0
 */
public interface SocketListener extends ConnectListener, DisconnectListener {

    /**
     * 连接建立时回调
     *
     * @param client 已连接的客户端
     */
    void onConnect(SocketIOClient client);

    /**
     * 断开连接时回调
     *
     * @param client 断开连接的客户端
     */
    void onDisconnect(SocketIOClient client);

    /**
     * 收到事件时回调
     *
     * @param client 已连接客户端
     * @param event  事件数据
     */
    void onEvent(SocketIOClient client, Event event);

    /**
     * 注册指定类型的事件监听器
     *
     * @param eventName  事件名称
     * @param eventClass 事件数据类型
     * @param listener   事件数据监听器
     * @param <T>        事件数据类型
     */
    <T> void onEvent(String eventName, Class<T> eventClass, DataListener<T> listener);

    /**
     * 发生错误时回调
     *
     * @param client 出错的客户端
     * @param error  异常信息
     */
    void onError(SocketIOClient client, Throwable error);
}
