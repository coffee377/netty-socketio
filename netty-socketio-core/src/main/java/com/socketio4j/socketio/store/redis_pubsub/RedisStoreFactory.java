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
package com.socketio4j.socketio.store.redis_pubsub;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socketio4j.socketio.store.Store;
import com.socketio4j.socketio.store.event.BaseStoreFactory;
import com.socketio4j.socketio.store.event.EventStore;
import com.socketio4j.socketio.store.event.EventStoreMode;

/**
 * Redis 会话存储工厂实现
 *
 * <p>使用 Redisson 作为驱动，将会话数据存储在 Redis 中，
 * 支持跨节点的会话数据共享。事件传播由 {@link EventStore} 决定，
 * 存储和事件分发系统可独立配置
 *
 * <p>默认使用 {@link RedisPubSubEventStore} 多通道模式：
 * <ul>
 *   <li>会话数据在 Redis 运行期间持久化</li>
 *   <li>事件广播到所有订阅节点</li>
 *   <li>Pub/Sub 语义，不支持事件回放</li>
 * </ul>
 *
 * <p>支持自定义 EventStore 实现混合拓扑，例如：
 * <pre>{@code
 * RedissonClient redis = Redisson.create();
 * EventStore es = new KafkaEventStore(...);
 * RedisStoreFactory factory = new RedisStoreFactory(redis, es);
 * }</pre>
 */

public class RedisStoreFactory extends BaseStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(RedisStoreFactory.class);

    private final RedissonClient redisClient;
    private final EventStore eventStore;

    /**
     * 创建使用指定 EventStore 的 RedisStoreFactory
     *
     * @param redisClient Redis 客户端（不能为 null）
     * @param eventStore  事件存储实现（不能为 null）
     */
    public RedisStoreFactory(@NotNull RedissonClient redisClient,
                                @NotNull EventStore eventStore) {
        this.redisClient = Objects.requireNonNull(redisClient, "redisClient cannot be null");
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore cannot be null");
    }

    /**
     * 创建使用默认 Redis 事件分发的 RedisStoreFactory
     *
     * <p>使用 {@link RedisPubSubEventStore} 多通道模式
     *
     * @param redisClient Redis 客户端（不能为 null）
     */
    public RedisStoreFactory(@NotNull RedissonClient redisClient) {
        this(redisClient,
             new RedisPubSubEventStore(redisClient, redisClient, EventStoreMode.MULTI_CHANNEL, null));
    }

    @Override
    public Store createStore(UUID sessionId) {
        return new RedisStore(sessionId, redisClient);
    }

    @Override
    public EventStore eventStore() {
        return eventStore;
    }

    @Override
    public <K, V> Map<K, V> createMap(String name) {
        return redisClient.getMap(name);
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
    public String toString() {
        return getClass().getSimpleName() + " (redis session store)";
    }
}
