package com.ccl.engineio.exception;

/**
 * 序列化异常
 *
 * <p>当将对象转换为字符串数据失败时抛出，通常由 {@link com.ccl.engineio.core.codec.StringCodec} 抛出</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 * @see DeserializationException
 * @see com.ccl.engineio.core.codec.StringCodec
 */
public class SerializationException extends EngineIOException {

    /**
     * 构造带消息的序列化异常
     *
     * @param message 异常消息
     */
    public SerializationException(String message) {
        super(message);
    }

    /**
     * 构造带消息和根因的序列化异常
     *
     * @param message 异常消息
     * @param cause   根因异常
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
