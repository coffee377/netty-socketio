package com.ccl.engineio.core.entity;

import java.util.List;

/**
 * Engine.IO 握手响应数据
 * 用于 OPEN (type 0) 数据包，包含服务端配置信息
 */
public class OpenData {

    /**
     * 会话 ID
     */
    private String sid;

    /**
     * 可升级的传输方式列表（如 ["websocket"]）
     */
    private List<String> upgrades;

    /**
     * 心跳间隔（毫秒）
     */
    private Integer pingInterval;

    /**
     * 心跳超时（毫秒）
     */
    private Integer pingTimeout;

    /**
     * 最大负载大小（字节）
     */
    private Integer maxPayload;

    /**
     * 默认构造函数
     */
    public OpenData() {
    }

    /**
     * 获取会话 ID
     * @return 会话 ID
     */
    public String getSid() {
        return sid;
    }

    /**
     * 设置会话 ID
     * @param sid 会话 ID
     */
    public void setSid(String sid) {
        this.sid = sid;
    }

    /**
     * 获取可升级的传输方式列表
     * @return 传输方式列表
     */
    public List<String> getUpgrades() {
        return upgrades;
    }

    /**
     * 设置可升级的传输方式列表
     * @param upgrades 传输方式列表
     */
    public void setUpgrades(List<String> upgrades) {
        this.upgrades = upgrades;
    }

    /**
     * 获取心跳间隔
     * @return 心跳间隔（毫秒）
     */
    public Integer getPingInterval() {
        return pingInterval;
    }

    /**
     * 设置心跳间隔
     * @param pingInterval 心跳间隔（毫秒）
     */
    public void setPingInterval(Integer pingInterval) {
        this.pingInterval = pingInterval;
    }

    /**
     * 获取心跳超时
     * @return 心跳超时（毫秒）
     */
    public Integer getPingTimeout() {
        return pingTimeout;
    }

    /**
     * 设置心跳超时
     * @param pingTimeout 心跳超时（毫秒）
     */
    public void setPingTimeout(Integer pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    /**
     * 获取最大负载大小
     * @return 最大负载大小（字节）
     */
    public Integer getMaxPayload() {
        return maxPayload;
    }

    /**
     * 设置最大负载大小
     * @param maxPayload 最大负载大小（字节）
     */
    public void setMaxPayload(Integer maxPayload) {
        this.maxPayload = maxPayload;
    }
}
