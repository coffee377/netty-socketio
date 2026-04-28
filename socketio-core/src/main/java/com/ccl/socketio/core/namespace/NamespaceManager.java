package com.ccl.socketio.core.namespace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NamespaceManager {

    private static final NamespaceManager INSTANCE = new NamespaceManager();
    private final Map<String, Namespace> namespaces = new ConcurrentHashMap<>();

    private NamespaceManager() {
        getOrCreateNamespace("/");
    }

    public static NamespaceManager getInstance() {
        return INSTANCE;
    }

    public Namespace getOrCreateNamespace(String name) {
        return namespaces.computeIfAbsent(name, k -> new Namespace(name));
    }

    public Namespace getNamespace(String name) {
        return namespaces.get(name);
    }

    public boolean hasNamespace(String name) {
        return namespaces.containsKey(name);
    }

    public void removeNamespace(String name) {
        if (!"/".equals(name)) {
            namespaces.remove(name);
        }
    }

    public Map<String, Namespace> getAllNamespaces() {
        return namespaces;
    }
}