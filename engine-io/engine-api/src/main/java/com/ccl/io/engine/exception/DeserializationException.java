package com.ccl.io.engine.exception;


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
