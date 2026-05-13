package com.ccl.io.engine.core.parser;

import com.ccl.io.engine.Parser;

/**
 * Engine.IO 解析器工厂
 *
 * <p>根据协议版本获取对应的解析器实例，支持 SPI 自动发现机制</p>
 *
 * @author coffee377
 * @see ParserDelegate
 */
public final class ParserFactory {

    /**
     * 默认委托解析器（单例）
     */
    public static final Parser INSTANCE = new ParserDelegate();

}
