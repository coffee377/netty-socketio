package com.ccl.io.engine;

import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.Transport;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Engine.IO 客户端会话接口
 *
 * <p>定义 Engine.IO 客户端的会话状态访问方法，包括协议版本、会话 ID、
 * 传输层类型、握手数据及连接状态等。所有 Engine.IO 客户端实现必须实现此接口</p>
 *
 * @author coffee377
 * @since 4.0.0
 */
public interface EngineClient extends EngineOperations {

    /**
     * 获取 Engine.IO 协议版本
     *
     * @return 协议版本号
     */
    int getEngineIOVersion();

    /**
     * 获取会话 ID
     *
     * @return 会话 ID
     */
    String getSessionId();

    /**
     * 获取传输层类型
     *
     * @return 传输层类型（WebSocket 或 Polling）
     */
    Transport getTransport();

    /**
     * 获取握手数据
     *
     * @return 握手阶段返回的配置数据
     */
    Handshake getHandshakeData();

    /**
     * 检查连接状态
     *
     * @return 原子布尔值，表示连接是否存活
     */
    AtomicBoolean isConnected();

    /**
     * 获取数据包队列
     *
     * <p>返回待处理的数据包队列，用于缓存尚未消费的数据包</p>
     *
     * @return 数据包队列
     */
    Queue<EngineIOPacket<?>> getEngineIOPacketQueue();

}
