package com.ccl.engineio.netty.transport;

import io.netty.buffer.ByteBuf;

import com.ccl.engineio.netty.transport.PollingTransport.PendingRequest;

/**
 * Polling 传输会话存储接口
 *
 * <p>管理 Engine.IO 长轮询传输所需的每个 Session 的待发送消息队列和挂起的 GET 请求。
 * 实现类需提供线程安全的 Session 维度数据管理。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface PollingSessionStore {

    /**
     * 入站待发送消息
     *
     * @param sessionId 会话 ID
     * @param data      待发送的消息数据
     */
    void enqueueOutput(String sessionId, ByteBuf data);

    /**
     * 出队一条待发送消息
     *
     * @param sessionId 会话 ID
     * @return 消息数据，队列为空时返回 null
     */
    ByteBuf pollOutput(String sessionId);

    /**
     * 检查是否有待发送消息
     *
     * @param sessionId 会话 ID
     * @return 有待发送消息时返回 true
     */
    boolean hasOutput(String sessionId);

    /**
     * 注册一个挂起的 GET 请求
     *
     * @param sessionId  会话 ID
     * @param request 挂起的 GET 请求
     */
    void registerPendingGet(String sessionId, PendingRequest request);

    /**
     * 取出一个挂起的 GET 请求
     *
     * @param sessionId 会话 ID
     * @return 挂起的 GET 请求，队列为空时返回 null
     */
    PendingRequest pollPendingGet(String sessionId);

    /**
     * 检查是否有挂起的 GET 请求
     *
     * @param sessionId 会话 ID
     * @return 有挂起请求时返回 true
     */
    boolean hasPendingGet(String sessionId);

    /**
     * 清除指定 Session 的所有数据（待发送消息和挂起 GET 请求）
     *
     * @param sessionId 会话 ID
     */
    void removeSession(String sessionId);
}
