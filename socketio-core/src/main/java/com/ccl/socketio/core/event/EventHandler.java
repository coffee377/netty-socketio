package com.ccl.socketio.core.event;

import com.ccl.socketio.core.namespace.Namespace;

public interface EventHandler {

    void onConnect(Namespace.SocketIOClient client);

    void onDisconnect(Namespace.SocketIOClient client);

    void onEvent(Namespace.SocketIOClient client, String eventName, Object... args);

    void onError(Namespace.SocketIOClient client, Throwable error);
}