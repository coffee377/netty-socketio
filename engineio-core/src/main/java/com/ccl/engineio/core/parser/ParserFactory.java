package com.ccl.engineio.core.parser;

import com.ccl.engineio.core.protocol.EngineVersion;

/**
 * Engine.IO 解析器工厂
 *
 * <p>根据协议版本获取对应的解析器实例</p>
 *
 * @author coffee377
 */
public final class ParserFactory {

    private static final Parser V4_PARSER = new ParserV4();

    private ParserFactory() {
    }

    /**
     * 根据协议版本获取解析器
     *
     * @param version Engine.IO 协议版本
     * @return 对应版本的解析器实例
     */
    public static Parser getParser(EngineVersion version) {
        if (version == null || version == EngineVersion.UNKNOWN) {
            return V4_PARSER;
        }
        switch (version) {
            case V2:
            case V3:
            case V4:
                return V4_PARSER;
            default:
                return V4_PARSER;
        }
    }

    /**
     * 根据版本号获取解析器
     *
     * @param version 版本号（如 "3"、"4"）
     * @return 对应版本的解析器实例
     */
    public static Parser getParser(String version) {
        return getParser(EngineVersion.fromValue(version));
    }

    /**
     * 获取默认解析器（V4）
     *
     * @return V4 协议解析器实例
     */
    public static Parser getDefaultParser() {
        return V4_PARSER;
    }
}
