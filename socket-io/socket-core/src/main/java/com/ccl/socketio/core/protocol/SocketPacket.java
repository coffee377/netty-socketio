package com.ccl.socketio.core.protocol;

import com.ccl.socketio.core.protocol.data.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.ccl.socketio.core.protocol.SocketPacket.Type.BINARY_ACK;
import static com.ccl.socketio.core.protocol.SocketPacket.Type.BINARY_EVENT;

/**
 * Socket.IO 协议数据包封装
 *
 * <p>Socket.IO 协议数据包格式：
 * <pre>[packet type][# of binary attachments-][namespace,][acknowledgment id][JSON payload]</pre>
 * </p>
 *
 * <p>当包含二进制附件时，数据包被拆分为多个 Engine.IO Message 包传输：
 * <ul>
 *   <li>第一个包：协议头 + JSON payload（不含二进制）</li>
 *   <li>后续包：每个二进制附件单独一个 Engine.IO Message</li>
 * </ul>
 * </p>
 *
 * @param <T> 数据包负载数据类型
 * @author coffee377
 * @see <a href="https://socket.io/zh-CN/docs/v4/socket-io-protocol/">Socket.IO 协议文档</a>
 * @since 4.0.0
 */
public class SocketPacket<T> {

    /**
     * 数据包类型
     */
    private final Type type;

    /**
     * 二进制附件数量
     */
    private final int attachmentsCount;

    /**
     * 命名空间，默认为 "/"
     */
    private final String namespace;

    /**
     * 确认 ID，用于 ACK 机制
     */
    private final Long ackId;

    private final String eventName;

    /**
     * 数据包负载
     */
    private final T data;

    /**
     * 二进制附件列表
     */
    private final List<byte[]> attachments;

    /**
     * 原始字符串数据，用于调试和回放
     */
    private final String rawSource;

    private SocketPacket(Builder<T> builder) {
        this.type = builder.type;
        this.attachmentsCount = builder.attachmentsCount;
        this.namespace = builder.namespace;
        this.ackId = builder.ackId;
        this.eventName = builder.eventName;
        this.data = builder.data;
        this.attachments = new ArrayList<>();
        this.rawSource = builder.rawSource;
    }

    /**
     * 创建数据包构建器
     *
     * @param <D> 负载数据类型
     * @return 新的 Builder 实例
     */
    public static <D> Builder<D> builder() {
        return builder(null);
    }

    /**
     * 创建数据包构建器，并指定原始数据源
     *
     * @param rawSource 原始字符串数据
     * @param <D>       负载数据类型
     * @return 新的 Builder 实例
     */
    public static <D> Builder<D> builder(String rawSource) {
        return new Builder<>(rawSource);
    }

    /**
     * 获取数据包类型
     *
     * @return 数据包类型枚举值
     */
    public Type getType() {
        return type;
    }

    /**
     * 获取二进制附件数量
     *
     * @return 附件数量，无附件时为 0
     */
    public int getAttachmentsCount() {
        return attachmentsCount;
    }

    /**
     * 获取命名空间
     *
     * @return 命名空间字符串，默认 "/"
     */
    public String getNamespace() {
        if (namespace == null || namespace.isEmpty()) {
            return "/";
        }
        return namespace;
    }

    /**
     * 获取确认 ID
     *
     * @return 确认 ID，无 ACK 时返回 null
     */
    public Long getAckId() {
        return ackId;
    }

    /**
     * 判断当前数据包是否请求了 ACK 确认
     *
     * @return 需要 ACK 确认时返回 true
     */
    public boolean isAckRequested() {
        return ackId != null;
    }

    /**
     * 获取事件名称
     *
     * @return 事件名称，非事件类型数据包返回 null
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * 获取数据包负载数据
     *
     * @return 负载数据，无数据时返回 null
     */
    public T getData() {
        return data;
    }

    /**
     * 获取二进制附件列表
     *
     * @return 二进制附件列表
     */
    public List<byte[]> getAttachments() {
        return attachments;
    }

    /**
     * 获取原始字符串数据
     *
     * @return 原始协议字符串，用于调试
     */
    public String getRawSource() {
        return rawSource;
    }

    /**
     * 添加二进制附件
     *
     * @param binaryAttachment 二进制数据
     */
    public void addAttachment(byte[] binaryAttachment) {
        if (this.attachments.size() < attachmentsCount) {
            this.attachments.add(binaryAttachment);
        }
    }

    /**
     * 判断是否包含二进制附件
     *
     * @return 包含附件时返回 true
     */
    public boolean hasAttachments() {
        return attachmentsCount != 0;
    }

    /**
     * 判断所有附件是否已加载
     *
     * @return 所有附件已加载时返回 true
     */
    public boolean isAttachmentsLoaded() {
        return this.attachments.size() == attachmentsCount;
    }

    /**
     * Socket.IO 数据包类型枚举
     */
    public enum Type {
        /**
         * 连接事件
         */
        CONNECT(0),

        /**
         * 断开连接事件
         */
        DISCONNECT(1),

        /**
         * 普通事件
         */
        EVENT(2),

        /**
         * 确认消息（ACK）
         */
        ACK(3),

        /**
         * 错误消息
         */
        ERROR(4),

        /**
         * 二进制事件（包含附件的事件）
         */
        BINARY_EVENT(5),

        /**
         * 二进制确认（包含附件的 ACK）
         */
        BINARY_ACK(6);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        /**
         * 获取类型编码值
         *
         * @return 类型对应的整数编码
         */
        public int getValue() {
            return value;
        }

        /**
         * 根据编码值获取类型枚举
         *
         * @param value 类型编码值
         * @return 对应的 Type 枚举值
         * @throws IllegalArgumentException 当编码值无效时
         */
        public static Type fromValue(int value) {
            for (Type type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown Socket.IO packet type: " + value);
        }
    }

    /**
     * SocketPacket 构建器
     *
     * @param <D> 负载数据类型
     */
    public static class Builder<D> {
        private Type type;
        private int attachmentsCount;
        private String namespace;
        private Long ackId;
        private String eventName;
        private D data;
        private String rawSource;

        private Builder(String rawSource) {
            this.rawSource = rawSource;
        }

        /**
         * 设置数据包类型
         *
         * @param type 数据包类型
         * @return 当前构建器
         */
        public Builder<D> type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * 设置二进制附件数量
         *
         * @param attachmentsCount 附件数量
         * @return 当前构建器
         */
        public Builder<D> attachmentsCount(int attachmentsCount) {
            this.attachmentsCount = attachmentsCount;
            return this;
        }

        /**
         * 设置命名空间
         *
         * @param namespace 命名空间
         * @return 当前构建器
         */
        public Builder<D> namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * 设置确认 ID
         *
         * @param ackId 确认 ID
         * @return 当前构建器
         */
        public Builder<D> ackId(long ackId) {
            this.ackId = ackId;
            return this;
        }

        public Builder<D> event(String name) {
            this.eventName = name;
            return this;
        }

        /**
         * 设置数据包负载数据
         *
         * @param data 负载数据
         * @param <T>  数据类型
         * @return 泛型化后的构建器
         */
        @SuppressWarnings("unchecked")
        public <T> Builder<T> data(T data) {
            this.data = (D) data;
            return (Builder<T>) this;
        }

        /**
         * 设置原始字符串数据
         *
         * @param rawSource 原始协议字符串
         * @return 当前构建器
         */
        public Builder<D> rawSource(String rawSource) {
            this.rawSource = rawSource;
            return this;
        }

        /**
         * 构建数据包实例
         *
         * @return 完整的 SocketPacket 实例
         */
        public SocketPacket<D> build() {
            return new SocketPacket<>(this);
        }
    }

}
