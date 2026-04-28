package com.ccl.socketio.core.namespace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class Namespace {

    private final String name;
    private final Map<String, CopyOnWriteArrayList<Consumer<SocketIOClient>>> eventHandlers;
    private final Map<String, CopyOnWriteArrayList<Consumer<SocketIOClient>>> ackCallbacks;

    public Namespace(String name) {
        this.name = name;
        this.eventHandlers = new ConcurrentHashMap<>();
        this.ackCallbacks = new ConcurrentHashMap<>();
    }

    public String getName() {
        return name;
    }

    public void on(String eventName, Consumer<SocketIOClient> handler) {
        eventHandlers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void off(String eventName, Consumer<SocketIOClient> handler) {
        CopyOnWriteArrayList<Consumer<SocketIOClient>> handlers = eventHandlers.get(eventName);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    public void emit(String eventName, SocketIOClient client, Object... args) {
        CopyOnWriteArrayList<Consumer<SocketIOClient>> handlers = eventHandlers.get(eventName);
        if (handlers != null) {
            for (Consumer<SocketIOClient> handler : handlers) {
                try {
                    handler.accept(client);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void registerAckCallback(int ackId, Consumer<Object> callback) {
        ackCallbacks.put(String.valueOf(ackId), new CopyOnWriteArrayList<>());
    }

    public void triggerAck(int ackId, Object data) {
        String key = String.valueOf(ackId);
        CopyOnWriteArrayList<Consumer<SocketIOClient>> callbacks = ackCallbacks.remove(key);
        if (callbacks != null) {
            for (Consumer<SocketIOClient> cb : callbacks) {
                try {
                    cb.accept(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class SocketIOClient {
        private final String sessionId;
        private final String namespace;

        public SocketIOClient(String sessionId, String namespace) {
            this.sessionId = sessionId;
            this.namespace = namespace;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getNamespace() {
            return namespace;
        }

        public void sendEvent(String eventName, Object... args) {
        }

        public void disconnect() {
        }
    }
}