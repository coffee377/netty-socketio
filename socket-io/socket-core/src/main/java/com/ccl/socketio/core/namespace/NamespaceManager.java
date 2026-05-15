package com.ccl.socketio.core.namespace;

import com.ccl.socketio.core.namespace.impl.Namespace;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket.IO 命名空间管理器
 *
 * <p>管理所有命名空间的创建、查找和销毁。
 * 使用 ConcurrentHashMap 保证线程安全，支持按名称访问命名空间。
 *
 * @author coffee377
 * @since 4.0.0
 */
public class NamespaceManager {

    private final Map<String, SocketIONamespace> namespaces = new ConcurrentHashMap<>();
    private static final NamespaceManager INSTANCE = new NamespaceManager();

    public NamespaceManager() {
    }

    /**
     * 获取 NamespaceManager 单例实例
     *
     * @return NamespaceManager 实例
     */
    public static NamespaceManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取或创建指定名称的命名空间
     *
     * @param name 命名空间名称
     * @return 命名空间实例
     */
    public SocketIONamespace getOrCreateNamespace(String name) {
        return namespaces.computeIfAbsent(name, k -> new Namespace(name));
    }

    /**
     * 获取指定名称的命名空间
     *
     * @param name 命名空间名称
     * @return 命名空间实例，不存在时返回 null
     */
    public SocketIONamespace getNamespace(String name) {
        return namespaces.get(name);
    }

    /**
     * 判断指定名称的命名空间是否已存在
     *
     * @param name 命名空间名称
     * @return 存在时返回 true
     */
    public boolean hasNamespace(String name) {
        return namespaces.containsKey(name);
    }

    /**
     * 移除指定名称的命名空间（默认命名空间 "/" 不会被移除）
     *
     * @param name 命名空间名称
     */
    public void remove(String name) {
        if (!"/".equals(name)) {
            SocketIONamespace namespace = namespaces.remove(name);
        }
    }

    /**
     * 获取所有已注册的命名空间
     *
     * @return 命名空间集合
     */
    public Collection<SocketIONamespace> getAllNamespaces() {
        return namespaces.values();
    }
}
