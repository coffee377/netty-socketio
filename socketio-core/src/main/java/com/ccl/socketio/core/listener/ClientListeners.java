package com.ccl.socketio.core.listener;

/**
 * Socket.IO 客户端监听器注册接口
 *
 * <p>定义各类事件监听器的注册方法，包括连接、断开、心跳和自定义事件。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface ClientListeners {

    /**
     * 添加连接事件监听器
     *
     * @param listener 连接事件监听器
     */
    void addConnectListener(ConnectListener listener);

    /**
     * 添加断开连接事件监听器
     *
     * @param listener 断开连接事件监听器
     */
    void addDisconnectListener(DisconnectListener listener);

    /**
     * 添加 Ping 心跳监听器
     *
     * @param listener Ping 心跳监听器
     */
    void addPingListener(PingListener listener);

    /**
     * 添加 Pong 心跳监听器
     *
     * @param listener Pong 心跳监听器
     */
    void addPongListener(PongListener listener);

    /**
     * 添加自定义事件监听器
     *
     * @param eventName 事件名称
     * @param eventClass 事件数据类型
     * @param listener  事件数据监听器
     * @param <T>       事件数据类型
     */
    <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener);

}
