package com.ccl.socketio.server.listener;

/**
 * Socket.IO 服务端事件监听器接口
 *
 * <p>定义服务端处理客户端生命周期事件（连接、断开、事件、错误）的回调方法。
 *
 * @author coffee377
 * @since 4.0.0
 */
public interface SocketIOListener {

    /**
     * 客户端连接回调
     *
     * @param sessionId 会话 ID
     * @param namespace 命名空间名称
     */
    void onConnect(String sessionId, String namespace);

    /**
     * 客户端断开连接回调
     *
     * @param sessionId 会话 ID
     * @param namespace 命名空间名称
     */
    void onDisconnect(String sessionId, String namespace);

    /**
     * 收到客户端事件回调
     *
     * @param sessionId 会话 ID
     * @param namespace 命名空间名称
     * @param eventName 事件名称
     * @param args      事件参数
     */
    void onEvent(String sessionId, String namespace, String eventName, Object... args);

    /**
     * 发生错误回调
     *
     * @param sessionId 会话 ID
     * @param namespace 命名空间名称
     * @param error     异常信息
     */
    void onError(String sessionId, String namespace, Throwable error);

    /**
     * 收到 Ping 心跳回调（可选实现）
     *
     * @param sessionId 会话 ID
     */
    default void onPing(String sessionId) {
    }

    /**
     * 收到 Pong 心跳回调（可选实现）
     *
     * @param sessionId 会话 ID
     */
    default void onPong(String sessionId) {
    }
}