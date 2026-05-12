package com.ccl.io.engine.protocol;

import com.ccl.io.engine.exception.EngineIOException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Engine.IO 协议数据包封装
 *
 * <p>支持泛型负载，包含类型标识与对应数据，用于 Engine.IO 协议层数据传输</p>
 *
 * @param <T> 数据包负载的数据类型
 * @author coffee377
 * @since 4.0.0
 */
public final class EngineIOPacket<T> {

    /**
     * 数据包类型标识
     */
    private final Type type;

    /**
     * 数据包负载数据
     */
    private final T data;

    /**
     * 数据类型（文本、二进制或对象）
     */
    private final DataType dataType;

    private EngineIOPacket(Builder<T> builder) {
        this.type = builder.type;
        this.data = builder.data;
        if (builder.data instanceof String) {
            this.dataType = DataType.PLAINTEXT;
        } else if (builder.data instanceof byte[]) {
            this.dataType = DataType.BINARY;
        } else {
            this.dataType = DataType.OBJECT;
        }
    }

    /**
     * 获取数据包负载数据
     *
     * @return 负载数据，无数据时返回 null
     */
    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        String data;
        if (this.data instanceof byte[]) {
            data = new String((byte[]) this.data, StandardCharsets.UTF_8);
        } else {
            data = Optional.ofNullable(this.data).map(Object::toString).orElse("");
        }
        return String.format("%d%s", this.type.getCode(), data);
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
     * 检查数据包负载是否为二进制类型
     *
     * @return 二进制数据时返回 true
     */
    public boolean isBinary() {
        return DataType.BINARY.equals(this.dataType);
    }

    /**
     * 检查数据包负载是否为文本类型
     *
     * @return 文本数据时返回 true
     */
    public boolean isText() {
        return DataType.PLAINTEXT.equals(this.dataType);
    }

    /**
     * 创建数据包构建器
     *
     * @param <D> 负载数据类型
     * @return 新的 Builder 实例，默认类型为 MESSAGE
     */
    public static <D> Builder<D> builder() {
        return new Builder<>();
    }

    /**
     * Engine.IO 协议数据包类型枚举
     *
     * <p>定义了 Engine.IO 协议中所有标准数据包类型，
     * 每个类型包含编码值、名称及用途说明</p>
     *
     * @see <a href="https://socket.io/zh-CN/docs/v4/engine-io-protocol/">Engine.IO 官方协议文档</a>
     */
    public enum Type {
        /**
         * 握手包，用于连接建立时的协议协商
         */
        OPEN(0, "open", "握手阶段使用"),

        /**
         * 关闭传输包，通知对端准备关闭连接
         */
        CLOSE(1, "close", "标识传输可关闭"),

        /**
         * 心跳请求包，服务端发送用于检测连接存活
         */
        PING(2, "ping", "心跳机制-服务端发送"),

        /**
         * 心跳响应包，客户端回复以确认连接正常
         */
        PONG(3, "pong", "心跳机制-客户端回复"),

        /**
         * 业务消息包，承载实际传输的数据负载
         */
        MESSAGE(4, "message", "传输业务负载数据"),

        /**
         * 传输升级包，用于传输层协议升级协商
         */
        UPGRADE(5, "upgrade", "传输升级流程"),

        /**
         * 空操作包，升级完成后清理长轮询资源
         */
        NOOP(6, "noop", "升级流程-清理长轮询");


        /**
         * 数据包类型编码值
         */
        private final int code;

        /**
         * 数据包类型名称
         */
        private final String name;

        /**
         * 类型用途说明
         */
        private final String description;

        /**
         * 构造数据包类型枚举实例
         *
         * @param code        类型编码值（0-6）
         * @param name        类型名称字符串
         * @param description 类型用途描述
         */
        Type(int code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        /**
         * 获取数据包类型编码值
         *
         * @return 类型对应的整数编码
         */
        public int getCode() {
            return code;
        }

        /**
         * 获取数据包类型用途描述
         *
         * @return 类型的用途说明文本
         */
        public String getDescription() {
            return description;
        }

        /**
         * 获取数据包类型名称
         *
         * @return 类型名称字符串
         */
        public String getName() {
            return name;
        }

        /**
         * 转换为字节（用于协议编码）
         *
         * <p>将数据包类型转换为 ASCII 字符字节，如 '0', '4' 等</p>
         *
         * @return 类型字符字节值
         */
        public byte getByte() {
            return getStringByte();
        }

        /**
         * 转换为字符串字节
         *
         * <p>将编码值加上 '0' 得到对应的 ASCII 字符字节</p>
         *
         * @return 字符串字节值
         */
        public byte getStringByte() {
            return (byte) (code + '0');
        }

        /**
         * 从字节值解析对应的数据包类型
         *
         * @param b 字节值（ASCII 字符，如 '0', '4'）
         * @return 对应的 Type 枚举值
         * @throws EngineIOException 当字节值无效时
         */
        public static Type fromByte(byte b) {
            int num = b - '0';
            if (num < OPEN.code || num > NOOP.code) {
                throw new EngineIOException("Invalid packet type byte: " + b);
            }
            for (Type value : values()) {
                if (value.getCode() == num) {
                    return value;
                }
            }
            return values()[num];
        }
    }

    /**
     * 数据包构建器
     *
     * <p>使用建造者模式创建 {@link EngineIOPacket} 实例，
     * 默认数据包类型为 MESSAGE</p>
     *
     * @param <D> 负载数据类型
     */
    public static class Builder<D> {
        private Type type;
        private D data;

        /**
         * 初始化构建器，默认类型为 MESSAGE
         */
        public Builder() {
            this.type = Type.MESSAGE;
        }

        /**
         * 设置数据包类型
         *
         * @param type 数据包类型枚举值
         * @return 当前构建器实例
         */
        public Builder<D> type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * 设置数据包负载数据
         *
         * <p>支持链式调用，返回泛型化的构建器实例</p>
         *
         * @param <T>  负载数据类型
         * @param data 负载数据
         * @return 泛型化后的构建器实例
         */
        @SuppressWarnings("unchecked")
        public <T> Builder<T> data(T data) {
            this.data = (D) data;
            return (Builder<T>) this;
        }

        /**
         * 构建数据包实例
         *
         * @return 完整的 EngineIOPacket 实例
         */
        public EngineIOPacket<D> build() {
            return new EngineIOPacket<>(this);
        }
    }
}
