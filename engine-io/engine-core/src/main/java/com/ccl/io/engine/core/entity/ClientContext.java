package com.ccl.io.engine.core.entity;

import com.ccl.io.engine.protocol.Transport;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Engine.IO 客户端上下文
 *
 * <p>持有单个客户端连接的状态信息，包括会话 ID、传输方式、连接状态等</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class ClientContext {

    /**
     * 会话唯一标识符
     */
    private final String sessionId;

    /**
     * 当前传输方式
     */
    private Transport transport;

    /**
     * 连接状态标识
     */
    private final AtomicBoolean connected;

    /**
     * 最后一次心跳时间戳（毫秒）
     */
    private long lastPingTime;

    /**
     * 客户端远程地址
     */
    private String remoteAddress;

    /**
     * 附加对象，用于扩展上下文信息
     */
    private Object attachment;

    /**
     * 创建客户端上下文，默认使用 POLLING 传输方式
     *
     * @param sessionId 会话 ID
     */
    public ClientContext(String sessionId) {
        this(sessionId, Transport.POLLING);
    }

    /**
     * 创建客户端上下文，指定传输方式
     *
     * @param sessionId     会话 ID
     * @param transport 传输方式
     */
    public ClientContext(String sessionId, Transport transport) {
        this.sessionId = sessionId;
        this.transport = transport;
        this.connected = new AtomicBoolean(true);
    }

    /**
     * 获取会话 ID
     *
     * @return 会话唯一标识符
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 获取当前传输方式
     *
     * @return 传输方式
     */
    public Transport getTransportType() {
        return transport;
    }

    /**
     * 设置传输方式
     *
     * @param transportType 传输方式
     */
    public void setTransportType(Transport transportType) {
        this.transport = transportType;
    }

    /**
     * 检查连接是否活跃
     *
     * @return 连接活跃时返回 true
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * 设置连接状态
     *
     * @param connected 连接状态
     */
    public void setConnected(boolean connected) {
        this.connected.set(connected);
    }

    /**
     * 断开连接，将连接状态标记为未连接
     */
    public void disconnect() {
        this.connected.set(false);
    }

    /**
     * 获取最后一次心跳时间
     *
     * @return 时间戳（毫秒）
     */
    public long getLastPingTime() {
        return lastPingTime;
    }

    /**
     * 设置最后一次心跳时间
     *
     * @param lastPingTime 时间戳（毫秒）
     */
    public void setLastPingTime(long lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    /**
     * 获取客户端远程地址
     *
     * @return 远程地址字符串
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * 设置客户端远程地址
     *
     * @param remoteAddress 远程地址字符串
     */
    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * 获取附加对象
     *
     * @return 附加对象，可能为 null
     */
    public Object getAttachment() {
        return attachment;
    }

    /**
     * 设置附加对象，用于扩展上下文信息
     *
     * @param attachment 附加对象
     */
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    /**
     * 升级传输方式为 WebSocket
     *
     * <p>从 HTTP 长轮询升级为 WebSocket 全双工通信</p>
     */
    public void upgradeTransport() {
        this.transport = Transport.WEBSOCKET;
    }
}
