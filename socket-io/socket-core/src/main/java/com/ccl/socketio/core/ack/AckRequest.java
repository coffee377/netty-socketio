package com.ccl.socketio.core.ack;

import com.ccl.socketio.core.namespace.SocketIOClient;
import com.ccl.socketio.core.protocol.SocketPacket;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Socket.IO ACK 确认请求封装
 *
 * <p>当收到需要确认的数据包时，通过此类发送确认响应。
 * 确保 ACK 只发送一次（通过 AtomicBoolean 保证）。
 *
 * @since 4.0.0-alpha.0
 */
public class AckRequest {

    private final SocketPacket<?> receivedPacket;
    private final SocketIOClient client;
    private final AtomicBoolean sent = new AtomicBoolean();

    /**
     * 创建 ACK 确认请求
     *
     * @param receivedPacket 待确认的原始数据包
     * @param client         确认响应发送的目标客户端
     */
    public AckRequest(SocketPacket<?> receivedPacket, SocketIOClient client) {
        this.receivedPacket = receivedPacket;
        this.client = client;
    }

    /**
     * 判断原始数据包是否请求了 ACK 确认
     *
     * @return 需要 ACK 确认时返回 true
     */
    public boolean isAckRequested() {
        return receivedPacket.isAckRequested();
    }

    /**
     * 发送 ACK 确认数据（可变参数版本）
     *
     * @param objs 确认数据
     */
    public void sendAckData(Object... objs) {
        sendAckData(Arrays.asList(objs));
    }

    /**
     * 发送 ACK 确认数据
     *
     * <p>根据原始数据包类型决定响应包类型（ACK 或 BINARY_EVENT），
     * 确保每个数据包只发送一次确认响应。
     *
     * @param objs 确认数据列表
     */
    public void sendAckData(List<Object> objs) {
        if (!isAckRequested() || !sent.compareAndSet(false, true)) {
            return;
        }

        SocketPacket.Type type = SocketPacket.Type.ACK;
        if (SocketPacket.Type.BINARY_EVENT.equals(receivedPacket.getType())) {
            type = SocketPacket.Type.BINARY_EVENT;
        }

        SocketPacket<List<Object>> ackPacket = SocketPacket.builder()
                .type(type)
                .namespace(receivedPacket.getNamespace())
                .ackId(receivedPacket.getAckId())
                .data(objs)
                .build();

        client.send(ackPacket);
    }

}
