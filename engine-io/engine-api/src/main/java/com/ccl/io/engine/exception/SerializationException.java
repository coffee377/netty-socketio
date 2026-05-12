package com.ccl.io.engine.exception;

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
