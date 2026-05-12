package com.ccl.io.engine;

import com.ccl.io.engine.protocol.EngineIOPacket;

import java.util.List;

/**
 * Engine.IO 客户端操作接口
 *
 * <p>定义 Engine.IO 连接的基本操作方法，包括数据发送和连接断开。
 * 所有 Engine.IO 客户端实现必须实现此接口</p>
 *
 * @author coffee377
 * @since 4.0.0
 */
public interface EngineOperations {

    /**
     * 发送单个数据包
     *
     * @param packet 待发送的数据包
     * @param <T>    数据包负载类型
     */
    <T> void send(EngineIOPacket<T> packet);

    /**
     * 批量发送数据包
     *
     * @param packets 待发送的数据包列表
     */
    void send(List<EngineIOPacket<?>> packets);

    /**
     * 断开当前连接
     */
    void disconnect();

}
