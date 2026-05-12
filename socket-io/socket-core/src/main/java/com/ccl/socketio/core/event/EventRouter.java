package com.ccl.socketio.core.event;

import com.ccl.socketio.core.namespace.impl.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Socket.IO 事件路由器
 *
 * <p>管理命名空间下的事件处理器注册、注销和路由分发。
 * 使用单例模式，内部通过双层 ConcurrentHashMap 按命名空间和事件名组织处理器。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class EventRouter {

    private final static Logger log = LoggerFactory.getLogger(EventRouter.class);
    private static final EventRouter INSTANCE = new EventRouter();
    private final Map<String, Map<String, Consumer<Namespace.SocketClient>>> eventHandlers = new ConcurrentHashMap<>();

    private EventRouter() {
    }

    /**
     * 获取 EventRouter 单例实例
     *
     * @return EventRouter 实例
     */
    public static EventRouter getInstance() {
        return INSTANCE;
    }

    /**
     * 注册命名空间下的事件处理器
     *
     * @param namespace 命名空间名称
     * @param eventName 事件名称
     * @param handler   事件处理回调
     */
    public void registerHandler(String namespace, String eventName, Consumer<Namespace.SocketClient> handler) {
        eventHandlers.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>()).put(eventName, handler);
    }

    /**
     * 注销命名空间下的事件处理器
     *
     * @param namespace 命名空间名称
     * @param eventName 事件名称
     */
    public void unregisterHandler(String namespace, String eventName) {
        Map<String, Consumer<Namespace.SocketClient>> nsHandlers = eventHandlers.get(namespace);
        if (nsHandlers != null) {
            nsHandlers.remove(eventName);
        }
    }

    /**
     * 将事件路由到对应的处理器
     *
     * @param namespace 命名空间名称
     * @param eventName 事件名称
     * @param client    事件来源客户端
     * @param args      事件参数
     */
    public void route(String namespace, String eventName, Namespace.SocketClient client, Object... args) {
        Map<String, Consumer<Namespace.SocketClient>> nsHandlers = eventHandlers.get(namespace);
        if (nsHandlers != null) {
            Consumer<Namespace.SocketClient> handler = nsHandlers.get(eventName);
            if (handler != null) {
                try {
                    handler.accept(client);
                } catch (Exception e) {
                    log.error("route event [{}] to namespace [{}] error", eventName, namespace, e);
                }
            }
        }
    }

    /**
     * 清除所有注册的事件处理器
     */
    public void clear() {
        eventHandlers.clear();
    }
}
