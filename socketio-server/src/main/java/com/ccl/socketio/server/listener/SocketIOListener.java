package com.ccl.socketio.server.listener;

public interface SocketIOListener {

    void onConnect(String sessionId, String namespace);

    void onDisconnect(String sessionId, String namespace);

    void onEvent(String sessionId, String namespace, String eventName, Object... args);

    void onError(String sessionId, String namespace, Throwable error);

    default void onPing(String sessionId) {
    }

    default void onPong(String sessionId) {
    }
}