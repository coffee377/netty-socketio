package com.ccl.engineio.exception;

/**
 * Engine.IO 基础异常类
 *
 * <p>所有 Engine.IO 相关异常的基类，继承自 {@link RuntimeException}，
 * 提供统一的异常层次结构以便调用方按需捕获</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class EngineIOException extends RuntimeException {

    public EngineIOException(String message) {
        super(message);
    }

    public EngineIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public EngineIOException(Throwable cause) {
        super(cause);
    }
}
