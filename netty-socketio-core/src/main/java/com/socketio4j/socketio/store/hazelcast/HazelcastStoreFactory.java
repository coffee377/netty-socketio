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
package com.socketio4j.socketio.store.hazelcast;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.socketio4j.socketio.store.Store;
import com.socketio4j.socketio.store.event.BaseStoreFactory;
import com.socketio4j.socketio.store.event.EventStore;
import com.socketio4j.socketio.store.event.EventStoreMode;

/**
 * Hazelcast 会话存储工厂实现
 *
 * <p>使用 Hazelcast 作为后端存储会话数据，事件传播由 {@link EventStore} 决定，
 * 支持混合配置：
 * <ul>
 *   <li>Hazelcast 会话 + Kafka 事件分发</li>
 *   <li>Hazelcast 会话 + Redis Streams 事件分发</li>
 *   <li>Hazelcast 会话 + 内存事件传播（仅本地）</li>
 * </ul>
 * 未指定 EventStore 时默认使用 {@link HazelcastPubSubEventStore}
 */
public class HazelcastStoreFactory extends BaseStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(HazelcastStoreFactory.class);

    private final HazelcastInstance hazelcastClient;
    private final EventStore eventStore;

    /**
     * 创建使用指定 EventStore 的 HazelcastStoreFactory
     *
     * @param hazelcastClient Hazelcast 实例（不能为 null）
     * @param eventStore      事件存储实现（不能为 null）
     * @throws NullPointerException 任一参数为 null 时抛出
     */
    public HazelcastStoreFactory(@NotNull HazelcastInstance hazelcastClient,
                                 @NotNull EventStore eventStore) {
        this.hazelcastClient = Objects.requireNonNull(hazelcastClient, "hazelcastClient cannot be null");
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore cannot be null");
    }

    /**
     * 创建使用默认 Hazelcast 事件分发的 HazelcastStoreFactory
     *
     * <p>使用 {@link HazelcastPubSubEventStore} 多通道模式
     *
     * @param hazelcastClient Hazelcast 实例（不能为 null）
     */
    public HazelcastStoreFactory(@NotNull HazelcastInstance hazelcastClient) {
        this(hazelcastClient,
             new HazelcastPubSubEventStore(hazelcastClient, hazelcastClient, null, null, null));
    }

    @Override
    public Store createStore(UUID sessionId) {
        return new HazelcastStore(sessionId, hazelcastClient);
    }

    @Override
    public EventStore eventStore() {
        return eventStore;
    }

    @Override
    public void shutdown() {
        try {
            eventStore.shutdown();
        } catch (Exception e) {
            log.error("Failed to shut down event store", e);
        }
    }

    @Override
    public <K, V> Map<K, V> createMap(String name) {
        return hazelcastClient.getMap(name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (Hazelcast session store)";
    }
}