package com.ccl.io.engine.protocol;

import java.util.Arrays;

/**
 * Engine.IO 协议版本枚举
 *
 * @author coffee377
 * @see <a href="https://github.com/socketio/engine.io-protocol">Engine.IO 协议仓库</a>
 */
public enum EngineIOVersion {

    /**
     * 未知版本
     */
    UNKNOWN(""),

    /**
     * Engine.IO 协议版本 2
     *
     * @link <a href="https://github.com/socketio/engine.io-protocol/tree/v2">Engine.IO version 2</a>
     */
    V2("2"),

    /**
     * Engine.IO 协议版本 3
     *
     * @link <a href="https://github.com/socketio/engine.io-protocol/tree/v3">Engine.IO version 3</a>
     */
    V3("3"),

    /**
     * Engine.IO 协议版本 4（当前推荐版本）
     *
     * @link <a href="https://github.com/socketio/engine.io-protocol/tree/main">Engine.IO version 4</a>
     */
    V4("4");

    /**
     * 版本号整数值
     */
    private final int intValue;

    /**
     * 版本号字符串值
     */
    private final String value;

    /**
     * Engine.IO 协议标识前缀（用于 URL 参数）
     */
    public static final String EIO = "EIO";

    /**
     * 构造函数
     *
     * @param value 版本号字符串值
     */
    EngineIOVersion(String value) {
        this.value = value;
        this.intValue = value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    /**
     * 获取版本号整数值
     *
     * @return 版本号整数值（如 2、3、4）
     */
    public int getValue() {
        return intValue;
    }

    /**
     * 获取版本号字符串值
     *
     * @return 版本号（如 "2"、"3"、"4"）
     */
    public String getStrValue() {
        return value;
    }

    /**
     * 根据版本字符串值转换为枚举
     *
     * @param value 版本号字符串
     * @return 对应的 EngineIOVersion 枚举值，若未找到则返回 UNKNOWN
     */
    public static EngineIOVersion fromValue(String value) {
        return Arrays.stream(values())
                .filter(v -> v.getStrValue().equals(value))
                .findFirst().orElse(UNKNOWN);
    }

}
