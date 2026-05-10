package com.ccl.socketio.server.config;

import java.util.Arrays;
import java.util.List;

/**
 * Socket.IO 服务端配置选项
 *
 * <p>包含端口、心跳参数、传输层、CORS 和 SSL 等配置项。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class ServerOptions {

    private int port = 3000;
    private long pingInterval = 25000;
    private long pingTimeout = 20000;
    private boolean allowCors = true;
    private String corsOrigin = "*";
    private List<String> transports = Arrays.asList("websocket", "polling");
    private int maxFramePayloadLength = 65536;
    private boolean enableSSL = false;
    private String keyCertChainPath;
    private String privateKeyPath;

    public ServerOptions() {
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(long pingInterval) {
        this.pingInterval = pingInterval;
    }

    public long getPingTimeout() {
        return pingTimeout;
    }

    public void setPingTimeout(long pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    public boolean isAllowCors() {
        return allowCors;
    }

    public void setAllowCors(boolean allowCors) {
        this.allowCors = allowCors;
    }

    public String getCorsOrigin() {
        return corsOrigin;
    }

    public void setCorsOrigin(String corsOrigin) {
        this.corsOrigin = corsOrigin;
    }

    public List<String> getTransports() {
        return transports;
    }

    public void setTransports(List<String> transports) {
        this.transports = transports;
    }

    public int getMaxFramePayloadLength() {
        return maxFramePayloadLength;
    }

    public void setMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
    }

    public boolean isEnableSSL() {
        return enableSSL;
    }

    public void setEnableSSL(boolean enableSSL) {
        this.enableSSL = enableSSL;
    }

    public String getKeyCertChainPath() {
        return keyCertChainPath;
    }

    public void setKeyCertChainPath(String keyCertChainPath) {
        this.keyCertChainPath = keyCertChainPath;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }
}