package com.ccl.socketio.core.protocol.data;

import java.util.List;

/**
 * Socket.IO ACK 确认参数封装
 *
 * <p>用于封装 ACK 确认消息的 ID 和数据负载。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class AckArgs {

    /**
     * ACK 确认 ID
     */
    private int ackId;

    /**
     * ACK 数据列表
     */
    private List<Object> data;

    public AckArgs() {
    }

    /**
     * 构造 ACK 参数实例
     *
     * @param ackId ACK 确认 ID
     * @param data  ACK 数据列表
     */
    public AckArgs(int ackId, List<Object> data) {
        this.ackId = ackId;
        this.data = data;
    }

    /**
     * 获取 ACK 确认 ID
     *
     * @return ACK 确认 ID
     */
    public int getAckId() {
        return ackId;
    }

    /**
     * 设置 ACK 确认 ID
     *
     * @param ackId ACK 确认 ID
     */
    public void setAckId(int ackId) {
        this.ackId = ackId;
    }

    /**
     * 获取 ACK 数据列表
     *
     * @return ACK 数据列表
     */
    public List<Object> getData() {
        return data;
    }

    /**
     * 设置 ACK 数据列表
     *
     * @param data ACK 数据列表
     */
    public void setData(List<Object> data) {
        this.data = data;
    }
}