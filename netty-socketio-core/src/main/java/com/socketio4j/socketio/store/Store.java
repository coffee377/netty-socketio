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


/**
 * 会话数据存储接口
 *
 * <p>每个 Store 实例与特定会话关联，提供键值存储操作，
 * 可基于不同后端实现（内存、Hazelcast、Redisson 等）
 */
public interface Store {

    /**
     * 设置指定键的值
     *
     * @param key 键名
     * @param val 要存储的值（不能为 null）
     * @throws NullPointerException 值为 null 时抛出
     */
    void set(String key, Object val);

    /**
     * 获取指定键的值
     *
     * @param <T> 值类型
     * @param key 键名
     * @return 键对应的值，不存在时返回 null
     */
    <T> T get(String key);

    /**
     * 检查指定键是否存在
     *
     * @param key 键名
     * @return 存在返回 true，否则返回 false
     */
    boolean has(String key);

    /**
     * 删除指定键的值
     *
     * @param key 键名
     */
    void del(String key);

    /**
     * 销毁此存储中的所有数据
     *
     * <p>在客户端断开连接时调用，调用后此存储实例不再可用。
     * 不同实现行为不同：分布式存储会删除整个 map，内存存储会清空数据
     */
    void destroy();

}
