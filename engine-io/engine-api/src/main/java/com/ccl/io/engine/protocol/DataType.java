package com.ccl.io.engine.protocol;

/**
 * Engine.IO 数据类型枚举
 *
 * @author coffee377
 * @see <a href="https://socket.io/zh-CN/docs/v4/engine-io-protocol/#message">Engine.IO 协议 - Message 格式</a>
 */
public enum DataType {

    /**
     * 文本数据
     */
    PLAINTEXT(0),

    /**
     * 二进制数据
     */
    BINARY(1),

    /**
     * 对象数据（JSON 格式）
     */
    OBJECT(2);

    /**
     * 数据类型数值
     */
    private final int value;

    /**
     * 构造函数
     *
     * @param value 数据类型数值
     */
    DataType(int value) {
        this.value = value;
    }

    /**
     * 获取数据类型数值
     *
     * @return 数据类型对应的整数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 从字节值转换为 DataType 枚举
     *
     * @param b 字节值
     * @return 对应的 DataType 枚举值
     */
    public static DataType fromByte(byte b) {
        return DataType.values()[b];
    }

    /**
     * 转换为字节值
     *
     * @return 当前枚举值对应的字节值
     */
    public byte toByte() {
        return (byte) value;
    }
}
