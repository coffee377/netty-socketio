package com.ccl.socketio.core.listener;

import com.ccl.socketio.core.ack.AckRequest;
import com.ccl.socketio.core.namespace.SocketIOClient;

/**
 * Socket.IO 数据事件监听器
 *
 * <p>当收到客户端发送的自定义事件数据时回调此接口。
 *
 * @param <T> 事件数据类型
 * @author coffee377
 * @since 4.0.0
 */
public interface DataListener<T> {

    /**
     * 收到事件数据时回调
     *
     * @param client    事件来源客户端
     * @param data      事件数据
     * @param ackRequest ACK 确认请求对象
     * @throws Exception 处理数据时可能抛出的异常
     */
    void onData(SocketIOClient client, T data, AckRequest AckRequest) throws Exception;

}
