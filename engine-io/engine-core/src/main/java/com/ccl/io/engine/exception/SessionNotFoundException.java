package com.ccl.io.engine.exception;

import com.ccl.io.engine.exception.EngineIOException;

/**
 * 会话未找到异常
 *
 * <p>当根据会话 ID 查找客户端会话时，目标会话不存在时抛出</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class SessionNotFoundException extends EngineIOException {

    /**
     * 未找到的会话 ID
     */
    private final String sessionId;

    /**
     * 根据会话 ID 构造异常
     *
     * @param sessionId 未找到的会话 ID
     */
    public SessionNotFoundException(String sessionId) {
        super("Session not found: " + sessionId);
        this.sessionId = sessionId;
    }

    /**
     * 根据会话 ID 和根因构造异常
     *
     * @param sessionId 未找到的会话 ID
     * @param cause     根因异常
     */
    public SessionNotFoundException(String sessionId, Throwable cause) {
        super("Session not found: " + sessionId, cause);
        this.sessionId = sessionId;
    }

    /**
     * 获取未找到的会话 ID
     *
     * @return 会话 ID
     */
    public String getSessionId() {
        return sessionId;
    }
}
