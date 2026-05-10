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
package com.socketio4j.socketio.store;

import java.util.Map;
import java.util.UUID;

import com.socketio4j.socketio.Disconnectable;
import com.socketio4j.socketio.handler.AuthorizeHandler;
import com.socketio4j.socketio.namespace.NamespacesHub;
import com.socketio4j.socketio.protocol.JsonSupport;
import com.socketio4j.socketio.store.event.EventStore;

/**
 * 存储工厂接口
 *
 * <p>创建客户端会话存储和事件存储，管理分布式数据结构和生命周期
 */
public interface StoreFactory extends Disconnectable {

    /**
     * 获取事件存储实例
     *
     * @return 事件存储
     */
    EventStore eventStore();

    /**
     * 创建指定名称的分布式 map
     *
     * @param <K>  键类型
     * @param <V>  值类型
     * @param name map 名称
     * @return 分布式 map
     */
    <K, V> Map<K, V> createMap(String name);

    /**
     * 创建指定会话的存储
     *
     * @param sessionId 会话 ID
     * @return 会话存储
     */
    Store createStore(UUID sessionId);

    /**
     * 初始化存储工厂
     *
     * @param namespacesHub    命名空间管理器
     * @param authorizeHandler 授权处理器
     * @param jsonSupport      JSON 支持
     */
    void init(NamespacesHub namespacesHub, AuthorizeHandler authorizeHandler, JsonSupport jsonSupport);

    /**
     * 关闭存储工厂，释放资源
     */
    void shutdown();

}
