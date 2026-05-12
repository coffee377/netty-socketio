package com.ccl.socketio.core.namespace;

import com.ccl.socketio.core.operations.BroadcastOperations;

import java.util.Collection;

/**
 * Socket.IO 命名空间接口
 *
 * <p>定义命名空间的核心行为，包括客户端管理、广播操作和监听器注册。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface SocketIONamespace {

    /**
     * 获取命名空间名称
     *
     * @return 命名空间名称
     */
    String getName();

    /**
     * 根据会话 ID 获取客户端
     *
     * @param sid 会话 ID
     * @return 客户端实例，不存在时返回 null
     */
    SocketIOClient getClient(String sid);

    /**
     * 添加客户端到命名空间
     *
     * @param client 客户端实例
     */
    void addClient(SocketIOClient client);

    /**
     * 从命名空间移除客户端
     *
     * @param sid 会话 ID
     * @return 被移除的客户端实例，不存在时返回 null
     */
    SocketIOClient removeClient(String sid);

    /**
     * 获取命名空间下所有客户端
     *
     * @return 客户端只读集合
     */
    Collection<SocketIOClient> getAllClients();

    /**
     * 获取广播操作对象
     *
     * @return 广播操作实例
     */
    BroadcastOperations getBroadcastOperations();

    /**
     * 获取指定房间的广播操作对象
     *
     * @param room 房间名称
     * @return 广播操作实例
     */
    BroadcastOperations getRoomOperations(String room);

    /**
     * 获取多个指定房间的广播操作对象
     *
     * @param rooms 房间名称列表
     * @return 广播操作实例
     */
    BroadcastOperations getRoomOperations(String... rooms);


    /**
     * 注册客户端事件监听
     *
     * @param eventName 事件名称
     * @param client    目标客户端
     */
    void on(String eventName, SocketIOClient client);

    /**
     * 移除客户端事件监听
     *
     * @param eventName 事件名称
     * @param client    目标客户端
     */
    void off(String eventName, SocketIOClient client);

    /**
     * 向客户端发送事件
     *
     * @param eventName 事件名称
     * @param client    目标客户端
     * @param args      事件参数
     */
    void emit(String eventName, SocketIOClient client, Object... args);

}
