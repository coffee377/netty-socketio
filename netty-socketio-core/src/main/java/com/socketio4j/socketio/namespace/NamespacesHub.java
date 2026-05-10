/**
 * Copyright (c) 2025 The Socketio4j Project
 * Parent project : Copyright (c) 2012-2025 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.socketio4j.socketio.namespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.SocketIOClient;
import com.socketio4j.socketio.SocketIONamespace;
import com.socketio4j.socketio.misc.CompositeIterable;

/**
 * 命名空间管理器
 *
 * <p>管理所有命名空间的创建、获取、删除，提供跨命名空间的房间客户端查询
 */
public class NamespacesHub {

    private final ConcurrentMap<String, SocketIONamespace> namespaces = new ConcurrentHashMap<>();
    private final Configuration configuration;

    public NamespacesHub(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * 创建或获取指定名称的命名空间
     *
     * @param name 命名空间名称
     * @return 命名空间实例
     */
    public Namespace create(String name) {
        Namespace namespace = (Namespace) namespaces.get(name);
        if (namespace == null) {
            namespace = new Namespace(name, configuration);
            Namespace oldNamespace = (Namespace) namespaces.putIfAbsent(name, namespace);
            if (oldNamespace != null) {
                namespace = oldNamespace;
            }
        }
        return namespace;
    }

    /**
     * 获取所有命名空间中指定房间的客户端
     *
     * @param room 房间名
     * @return 客户端迭代器
     */
    public Iterable<SocketIOClient> getRoomClients(String room) {
        List<Iterable<SocketIOClient>> allClients = new ArrayList<Iterable<SocketIOClient>>();
        for (SocketIONamespace namespace : namespaces.values()) {
            Iterable<SocketIOClient> clients = ((Namespace) namespace).getRoomClients(room);
            allClients.add(clients);
        }
        return new CompositeIterable<>(allClients);
    }

    /**
     * 获取指定名称的命名空间
     *
     * @param name 命名空间名称
     * @return 命名空间，不存在时返回 null
     */
    public Namespace get(String name) {
        return (Namespace) namespaces.get(name);
    }

    /**
     * 移除指定名称的命名空间并断开其所有客户端
     *
     * @param name 命名空间名称
     */
    public void remove(String name) {
        SocketIONamespace namespace = namespaces.remove(name);
        if (namespace != null) {
            namespace.getBroadcastOperations().disconnect();
        }
    }

    /**
     * 获取所有命名空间
     *
     * @return 命名空间集合
     */
    public Collection<SocketIONamespace> getAllNamespaces() {
        return namespaces.values();
    }

}
