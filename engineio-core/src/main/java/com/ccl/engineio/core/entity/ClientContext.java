package com.ccl.engineio.core.entity;

import com.ccl.engineio.core.protocol.TransportType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Engine.IO 客户端上下文
 *
 * <p>持有单个客户端连接的状态信息，包括会话 ID、传输方式、连接状态等。
 */
public class ClientContext {

    private final String sessionId;

    private TransportType transportType;

    private final AtomicBoolean connected;

    private long lastPingTime;

    private String remoteAddress;

    private Object attachment;

    public ClientContext(String sessionId) {
        this(sessionId, TransportType.POLLING);
    }

    public ClientContext(String sessionId, TransportType transportType) {
        this.sessionId = sessionId;
        this.transportType = transportType;
        this.connected = new AtomicBoolean(true);
    }

    public String getSessionId() {
        return sessionId;
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void setConnected(boolean connected) {
        this.connected.set(connected);
    }

    /**
     * 断开连接。
     */
    public void disconnect() {
        this.connected.set(false);
    }

    public long getLastPingTime() {
        return lastPingTime;
    }

    public void setLastPingTime(long lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    /**
     * 升级传输方式为 WebSocket。
     */
    public void upgradeTransport() {
        this.transportType = TransportType.WEBSOCKET;
    }
}
