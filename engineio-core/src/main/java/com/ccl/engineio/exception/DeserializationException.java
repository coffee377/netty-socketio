package com.ccl.engineio.exception;

import com.ccl.engineio.core.codec.Codec;

/**
 * 反序列化异常
 *
 * <p>当将字符串数据解析为对象失败时抛出，通常由 {@link Codec} 抛出</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 * @see SerializationException
 * @see Codec
 */
public class DeserializationException extends EngineIOException {

    /**
     * 构造带消息的反序列化异常
     *
     * @param message 异常消息
     */
    public DeserializationException(String message) {
        super(message);
    }

    /**
     * 构造带消息和根因的反序列化异常
     *
     * @param message 异常消息
     * @param cause   根因异常
     */
    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
