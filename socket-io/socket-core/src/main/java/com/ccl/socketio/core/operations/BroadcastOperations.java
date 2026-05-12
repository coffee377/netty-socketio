package com.ccl.socketio.core.operations;

import com.ccl.socketio.core.namespace.SocketIOClient;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Socket.IO 广播操作接口
 *
 * <p>定义向多个客户端广播事件的能力，支持排除指定客户端或按条件过滤接收方。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface BroadcastOperations extends ClientOperations {

    /**
     * 获取广播目标客户端集合
     *
     * @return 客户端集合
     */
    Collection<SocketIOClient> getClients();

//    <T> void send(Packet packet, BroadcastAckCallback<T> ackCallback);

    /**
     * 向指定客户端发送事件（排除当前发送者）
     *
     * @param name   事件名称
     * @param client 排除的客户端
     * @param data   事件数据
     */
    void sendEvent(String name, SocketIOClient client, Object... data);

    /**
     * 向符合条件的客户端发送事件
     *
     * @param name             事件名称
     * @param clientPredicate 客户端过滤条件
     * @param data             事件数据
     */
    void sendEvent(String name, Predicate<SocketIOClient> clientPredicate, Object... data);

//    <T> void sendEvent(String name, Object data, BroadcastAckCallback<T> ackCallback);

//    <T> void sendEvent(String name, Object data, SocketIOClient client, BroadcastAckCallback<T> ackCallback);

//    <T> void sendEvent(String name, Object data, Predicate<SocketIOClient> clientPredicate, BroadcastAckCallback<T> ackCallback);

}
