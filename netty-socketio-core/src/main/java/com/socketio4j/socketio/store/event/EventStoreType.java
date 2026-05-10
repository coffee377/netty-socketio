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
package com.socketio4j.socketio.store.event;

/**
 * 事件存储类型枚举
 *
 * <p>LOCAL：单节点内存存储
 * PUBSUB：广播式非持久存储
 * STREAM：广播式可回放存储
 * BROKER：基于队列的点对点存储
 */
public enum EventStoreType {
    LOCAL,   // Single-node, in-memory
    PUBSUB,  // Broadcast, non-durable
    STREAM,  // Broadcast, durable/replayable
    BROKER   // Queue-based, point-to-point
}
