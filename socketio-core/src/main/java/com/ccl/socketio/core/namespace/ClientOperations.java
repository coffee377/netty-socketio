package com.ccl.socketio.core.namespace;

import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.core.protocol.data.Event;

/**
 * Socket.IO 客户端操作接口
 *
 * <p>定义服务端对已连接客户端的操作能力，包括发送数据包、发送事件和断开连接。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface ClientOperations {

    /**
     * 发送自定义数据包
     *
     * <p>大多数场景下使用 {@link #sendEvent} 即可满足需求，
     * 此方法用于需要直接控制数据包内容的底层场景。
     * </p>
     *
     * @param packet 待发送的 SocketPacket 数据包
     * @param <T>    数据包负载数据类型
     */
    <T> void send(SocketPacket<T> packet);

    /**
     * 断开客户端连接
     */
    void disconnect();

    /**
     * 发送事件消息
     *
     * @param name 事件名称
     * @param data 事件数据
     */
    default void sendEvent(String name, Object... data) {
        Event event = new Event();
        event.setName(name);
        sendEvent(event);
    }

    /**
     * 发送事件消息
     *
     * @param event 事件对象（包含事件名和参数）
     */
    void sendEvent(Event event);

}
