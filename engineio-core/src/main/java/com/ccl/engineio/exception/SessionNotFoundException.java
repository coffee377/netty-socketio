package com.ccl.engineio.exception;

public class SessionNotFoundException extends EngineIOException {

    private final String sessionId;

    public SessionNotFoundException(String sessionId) {
        super("Session not found: " + sessionId);
        this.sessionId = sessionId;
    }

    public SessionNotFoundException(String sessionId, Throwable cause) {
        super("Session not found: " + sessionId, cause);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
