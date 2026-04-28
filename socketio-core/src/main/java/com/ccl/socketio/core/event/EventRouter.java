package com.ccl.socketio.core.event;

import com.ccl.socketio.core.namespace.Namespace;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventRouter {

    private static final EventRouter INSTANCE = new EventRouter();
    private final Map<String, Map<String, Consumer<Namespace.SocketIOClient>>> eventHandlers = new ConcurrentHashMap<>();

    private EventRouter() {
    }

    public static EventRouter getInstance() {
        return INSTANCE;
    }

    public void registerHandler(String namespace, String eventName, Consumer<Namespace.SocketIOClient> handler) {
        eventHandlers.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>()).put(eventName, handler);
    }

    public void unregisterHandler(String namespace, String eventName) {
        Map<String, Consumer<Namespace.SocketIOClient>> nsHandlers = eventHandlers.get(namespace);
        if (nsHandlers != null) {
            nsHandlers.remove(eventName);
        }
    }

    public void route(String namespace, String eventName, Namespace.SocketIOClient client, Object... args) {
        Map<String, Consumer<Namespace.SocketIOClient>> nsHandlers = eventHandlers.get(namespace);
        if (nsHandlers != null) {
            Consumer<Namespace.SocketIOClient> handler = nsHandlers.get(eventName);
            if (handler != null) {
                try {
                    handler.accept(client);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void clear() {
        eventHandlers.clear();
    }
}