package com.ccl.socketio.core.namespace.impl;

import com.ccl.socketio.core.listener.*;
import com.ccl.socketio.core.namespace.SocketIOClient;
import com.ccl.socketio.core.namespace.SocketIONamespace;
import com.ccl.socketio.core.operations.BroadcastOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Socket.IO 命名空间默认实现
 *
 * <p>管理特定命名空间下的客户端连接、房间和事件处理。
 * 采用 ConcurrentHashMap 保证并发安全，支持多线程环境下的客户端增删和事件分发。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class Namespace implements SocketIONamespace {
    private static final Logger log = LoggerFactory.getLogger(Namespace.class);

    private final String name;
    private final Map<String, SocketIOClient> allClients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> roomClients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> clientRooms = new ConcurrentHashMap<>();

    private final Map<String, CopyOnWriteArrayList<Consumer<SocketClient>>> eventHandlers;
    private final Map<String, CopyOnWriteArrayList<Consumer<SocketClient>>> ackCallbacks;

    /**
     * 创建命名空间实例
     *
     * @param name 命名空间名称
     */
    public Namespace(String name) {
        this.name = name;
        this.eventHandlers = new ConcurrentHashMap<>();
        this.ackCallbacks = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }



    @Override
    public SocketIOClient getClient(String sid) {
        return allClients.get(sid);
    }

    @Override
    public void addClient(SocketIOClient client) {
        allClients.put(client.getSessionId(), client);
    }

    @Override
    public SocketIOClient removeClient(String sid) {
        SocketIOClient client = getClient(sid);
        if (client != null) {
            allClients.remove(sid);
            // roomClients.remove(sid);
            // clientRooms.remove(sid);
            return client;
        }
        return null;
    }

    @Override
    public Collection<SocketIOClient> getAllClients() {
        return Collections.unmodifiableCollection(allClients.values());
    }

    @Override
    public BroadcastOperations getBroadcastOperations() {
        return null;
    }

    @Override
    public BroadcastOperations getRoomOperations(String room) {
        return null;
    }

    @Override
    public BroadcastOperations getRoomOperations(String... rooms) {
        return null;
    }

    @Override
    public void addConnectListener(ConnectListener listener) {

    }

    @Override
    public void addDisconnectListener(DisconnectListener listener) {

    }

    @Override
    public void addPingListener(PingListener listener) {

    }

    @Override
    public void addPongListener(PongListener listener) {

    }

    @Override
    public <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener) {

    }

    /**
     * 注册事件处理器
     *
     * @param eventName 事件名称
     * @param handler   事件处理回调
     */
    public void on(String eventName, Consumer<SocketClient> handler) {
        eventHandlers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * 注销事件处理器
     *
     * @param eventName 事件名称
     * @param handler   待注销的事件处理回调
     */
    public void off(String eventName, Consumer<SocketClient> handler) {
        CopyOnWriteArrayList<Consumer<SocketClient>> handlers = eventHandlers.get(eventName);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * 触发指定事件
     *
     * @param eventName 事件名称
     * @param client    事件来源客户端
     * @param args      事件参数
     */
    public void emit(String eventName, SocketClient client, Object... args) {
        CopyOnWriteArrayList<Consumer<SocketClient>> handlers = eventHandlers.get(eventName);
        if (handlers != null) {
            for (Consumer<SocketClient> handler : handlers) {
                try {
                    handler.accept(client);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 注册 ACK 回调
     *
     * @param ackId    ACK 确认 ID
     * @param callback ACK 回调函数
     */
    public void registerAckCallback(int ackId, Consumer<Object> callback) {
        ackCallbacks.put(String.valueOf(ackId), new CopyOnWriteArrayList<>());
    }

    /**
     * 触发指定 ACK 的回调
     *
     * @param ackId ACK 确认 ID
     * @param data   ACK 数据
     */
    public void triggerAck(Long ackId, Object data) {
        String key = String.valueOf(ackId);
        CopyOnWriteArrayList<Consumer<SocketClient>> callbacks = ackCallbacks.remove(key);
        if (callbacks != null) {
            for (Consumer<SocketClient> cb : callbacks) {
                try {
                    cb.accept(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Socket.IO 客户端封装（已废弃）
     *
     * @deprecated 将使用 {@link com.ccl.socketio.core.namespace.impl.NamespaceClient} 替代
     */
    @Deprecated
    public static class SocketClient {
        private final String sessionId;
        private final String namespace;

        public SocketClient(String sessionId, String namespace) {
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
