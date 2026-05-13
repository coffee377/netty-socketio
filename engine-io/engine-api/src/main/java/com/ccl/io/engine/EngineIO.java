package com.ccl.io.engine;

import com.ccl.io.engine.codec.Codec;
import com.ccl.io.engine.codec.EngineIODecoder;
import com.ccl.io.engine.codec.EngineIOEncoder;

/**
 * Engine.IO 编解码器基础接口
 *
 * <p>为所有 Engine.IO 编解码器提供公共常量和协议版本支持检查，
 * 所有 {@link EngineIODecoder} 和 {@link EngineIOEncoder} 实现必须继承此接口</p>
 *
 * @author coffee377
 * @see EngineIODecoder
 * @see EngineIOEncoder
 * @since 4.0.0
 */
public interface EngineIO {

    /**
     * V4 协议记录分隔符（0x1E）
     *
     * <p>用于 V4 协议中多个数据包 Payload 的分隔标识</p>
     */
    byte V4_RECORD_SEPARATOR = 0x001E;

    /**
     * 检查当前编解码器是否支持指定协议版本
     *
     * @param protocolVersion 协议版本号
     * @return 如果支持该版本则返回 true，否则返回 false
     */
    default boolean isSupport(int protocolVersion) {
        return true;
    }

    /**
     * 获取编解码器实例
     *
     * @return 编解码器
     */
    default Codec getCodec() {
        return Codec.NOOP;
    }

}
