package com.ccl.engineio.core.protocol;

import java.util.Base64;

/**
 * Engine.IO 数据包封装类
 *
 * @param <T> 数据类型
 * @author coffee377
 */
public final class EngineIOPacket<T> {

    /**
     * 数据包类型
     */
    private final Type type;

    /**
     * 数据
     */
    private final T data;

    /**
     * 私有构造函数，使用 of 工厂方法创建实例
     *
     * @param type 数据包类型
     * @param data 数据
     */
    private EngineIOPacket(Type type, T data) {
        this.type = type;
        this.data = data;
    }

    /**
     * 创建指定类型和数据的包
     *
     * @param type 数据包类型
     * @param data 数据
     * @param <D>  数据类型
     * @return EngineIOPacket 实例
     */
    public static <D> EngineIOPacket<D> of(Type type, D data) {
        return new EngineIOPacket<>(type, data);
    }

    /**
     * 创建指定类型的空包（无数据）
     *
     * @param type 数据包类型
     * @param <D>  数据类型
     * @return EngineIOPacket 实例
     */
    public static <D> EngineIOPacket<D> of(Type type) {
        return new EngineIOPacket<>(type, null);
    }

    /**
     * 创建 MESSAGE 类型的消息包
     *
     * @param data 消息数据
     * @param <D>  数据类型
     * @return MESSAGE 类型的 EngineIOPacket 实例
     */
    public static <D> EngineIOPacket<D> of(D data) {
        return new EngineIOPacket<>(Type.MESSAGE, data);
    }

    /**
     * 从字节数组创建数据包
     *
     * <p>解析规则：
     * <ul>
     *   <li>'b' 前缀：Base64 编码的二进制数据（作为 MESSAGE 类型）</li>
     *   <li>类型字节 + 数据：数据包类型 + 负载数据</li>
     *   <li>其他：整个字节数组作为 MESSAGE 类型数据</li>
     * </ul>
     *
     * @param byteData 字节数据
     * @param dataType 数据类型
     * @return EngineIOPacket 实例
     */
    public static EngineIOPacket<?> fromBytes(byte[] byteData, DataType dataType) {
        if (byteData == null || byteData.length == 0) {
            return of(Type.MESSAGE, null);
        }

        int firstInt = byteData[0] & 0xFF;

        if (firstInt == 'b') {
            byte[] data = Base64.getDecoder().decode(new String(byteData, 1, byteData.length - 1));
            return of(data);
        }

        if (firstInt >= (Type.OPEN.getStringByte() & 0xFF) && firstInt <= (Type.NOOP.getStringByte() & 0xFF)) {
            Type type = Type.fromByte((byte) firstInt);
            if (byteData.length > 1) {
                byte[] data = new byte[byteData.length - 1];
                System.arraycopy(byteData, 1, data, 0, data.length);
                return of(type, data);
            }
            return of(type);
        }

        return of(byteData);
    }

    /**
     * 获取数据包数据
     *
     * @return 数据
     */
    public T getData() {
        return data;
    }

    /**
     * 获取数据包类型
     *
     * @return 数据包类型枚举
     */
    public Type getType() {
        return type;
    }

    /**
     * Engine.IO v4 数据包类型枚举
     *
     * @see <a href="https://socket.io/zh-CN/docs/v4/engine-io-protocol/">官方协议文档</a>
     */
    public enum Type {
        /**
         * 握手包
         */
        OPEN(0, "open", "握手阶段使用"),

        /**
         * 关闭传输包
         */
        CLOSE(1, "close", "标识传输可关闭"),

        /**
         * 心跳请求包
         */
        PING(2, "ping", "心跳机制-服务端发送"),

        /**
         * 心跳响应包
         */
        PONG(3, "pong", "心跳机制-客户端回复"),

        /**
         * 业务消息包
         */
        MESSAGE(4, "message", "传输业务负载数据"),

        /**
         * 传输升级包
         */
        UPGRADE(5, "upgrade", "传输升级流程"),

        /**
         * 空操作包（升级清理）
         */
        NOOP(6, "noop", "升级流程-清理长轮询");


        /**
         * 数据包类型编码
         */
        private final int code;

        /**
         * 数据包类型名称
         */
        private final String name;

        /**
         * 用途说明
         */
        private final String description;

        /**
         * 构造函数
         *
         * @param code        数据包类型编码
         * @param name        数据包类型名称
         * @param description 用途说明
         */
        Type(int code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        /**
         * 获取数据包类型编码
         *
         * @return 数据包类型对应的整数值
         */
        public int getCode() {
            return code;
        }

        /**
         * 获取数据包类型描述
         *
         * @return 数据包类型的用途说明
         */
        public String getDescription() {
            return description;
        }

        /**
         * 获取数据包类型名称
         *
         * @return 数据包类型名称
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
         * 转换为二进制字节（保留低4位）
         *
         * @return 二进制字节值
         */
        public byte getBinaryByte() {
            return (byte) (code & 0x0F);
        }

        /**
         * 转换为字符串字节
         *
         * @return 字符串字节值
         */
        public byte getStringByte() {
            return (byte) (code + '0');
        }

        /**
         * 从字节值转换为 PacketType
         *
         * @param b 字节值
         * @return 对应的 PacketType 枚举值
         */
        public static Type fromByte(byte b) {
            int index = b - '0';
            if (index < 0 || index >= values().length) {
                throw new ArrayIndexOutOfBoundsException("Invalid packet type byte: " + b);
            }
            return values()[index];
        }
    }
}
