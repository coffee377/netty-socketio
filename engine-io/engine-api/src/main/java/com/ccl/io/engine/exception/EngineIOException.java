package com.ccl.io.engine.exception;

/**
 * Engine.IO 异常基类
 *
 * <p>所有 Engine.IO 相关的异常都继承自此类，包括编解码错误、
 * 协议解析错误、网络错误等</p>
 *
 * @author coffee377
 * @since 4.0.0
 */
public class EngineIOException extends RuntimeException {

    /**
     * 无参构造函数
     */
    public EngineIOException() {
        super();
    }

    /**
     * 构造带消息的异常
     *
     * @param message 异常消息
     */
    public EngineIOException(String message) {
        super(message);
    }

    /**
     * 构造带消息和根因的异常
     *
     * @param message 异常消息
     * @param cause   根因异常
     */
    public EngineIOException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造带根因的异常
     *
     * @param cause 根因异常
     */
    public EngineIOException(Throwable cause) {
        super(cause);
    }
}
