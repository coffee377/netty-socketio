package com.ccl.engineio.core.entity;

import com.ccl.engineio.core.protocol.TransportType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Engine.IO 客户端上下文
 * 持有单个客户端连接的状态信息，包括会话 ID、传输方式、连接状态等
 */
public class ClientContext {

    /** 会话 ID（唯一标识客户端） */
    private final String sessionId;

    /** 当前传输方式（polling 或 websocket） */
    private TransportType transportType;

    /** 连接状态（true 表示已连接） */
    private final AtomicBoolean connected;

    /** 最后一次心跳时间戳（毫秒） */
    private long lastPingTime;

    /** 客户端远程地址 */
    private String remoteAddress;

    /** 自定义附加数据（用于存储认证信息等） */
    private Object attachment;

    /**
     * 创建客户端上下文
     * @param sessionId 会话 ID
     */
    public ClientContext(String sessionId) {
        this.sessionId = sessionId;
        this.connected = new AtomicBoolean(true);
    }

    /** 获取会话 ID
     * @return 会话 ID */
    public String getSessionId() {
        return sessionId;
    }

    /** 获取当前传输方式
     * @return 传输方式 */
    public TransportType getTransportType() {
        return transportType;
    }

    /** 设置传输方式
     * @param transportType 传输方式 */
    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    /** 检查客户端是否已连接
     * @return true 表示已连接 */
    public boolean isConnected() {
        return connected.get();
    }

    /** 设置连接状态
     * @param connected 连接状态 */
    public void setConnected(boolean connected) {
        this.connected.set(connected);
    }

    /** 断开连接 */
    public void disconnect() {
        this.connected.set(false);
    }

    /** 获取最后心跳时间
     * @return 时间戳（毫秒） */
    public long getLastPingTime() {
        return lastPingTime;
    }

    /** 设置最后心跳时间
     * @param lastPingTime 时间戳（毫秒） */
    public void setLastPingTime(long lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    /** 获取远程地址
     * @return 远程地址字符串 */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /** 设置远程地址
     * @param remoteAddress 远程地址字符串 */
    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /** 获取自定义附加数据
     * @return 附加数据对象 */
    public Object getAttachment() {
        return attachment;
    }

    /** 设置自定义附加数据
     * @param attachment 附加数据对象 */
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    /** 升级传输方式为 WebSocket */
    public void upgradeTransport() {
        this.transportType = TransportType.WEBSOCKET;
    }
}
