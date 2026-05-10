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
package com.socketio4j.socketio.store.memory;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.socketio4j.socketio.store.Store;
import com.socketio4j.socketio.store.event.BaseStoreFactory;
import com.socketio4j.socketio.store.event.EventStore;

/**
 * 内存会话存储工厂实现
 *
 * <p>会话数据通过 {@link MemoryStore} 存储在本地 JVM 内存中，
 * 事件传播由 {@link EventStore} 决定，支持混合配置：
 * <ul>
 *   <li>内存会话 + Kafka 事件分发</li>
 *   <li>内存会话 + Redis Streams 事件分发</li>
 *   <li>内存会话 + 内存事件传播（仅本地）</li>
 * </ul>
 * 未指定 EventStore 时默认使用 {@link MemoryEventStore}
 */
public class MemoryStoreFactory extends BaseStoreFactory {

    private final EventStore eventStore;

    /**
     * 创建使用 {@link MemoryEventStore} 的工厂实例
     *
     * <p>会话数据和事件均限制在本地 JVM 中
     */
    public MemoryStoreFactory() {
        this.eventStore = new MemoryEventStore();
    }

    /**
     * 创建使用指定 EventStore 的工厂实例
     *
     * <p>会话数据保持本地存储，事件传播取决于给定的 EventStore 实现
     *
     * @param eventStore 事件存储实现（不能为 null）
     * @throws NullPointerException eventStore 为 null 时抛出
     */
    public MemoryStoreFactory(@NotNull EventStore eventStore) {
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore can not be null");
    }

    @Override
    public Store createStore(UUID sessionId) {
        return new MemoryStore();
    }

    @Override
    public EventStore eventStore() {
        return eventStore;
    }


    @Override
    public void shutdown() {
        // no-op
    }

    @Override
    public <K, V> Map<K, V> createMap(String name) {
        return new ConcurrentHashMap<>();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (memory session store)";
    }
}
