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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 事件发布配置
 *
 * <p>定义默认发布模式及各事件类型的覆盖模式
 */
public class PublishConfig {

    private final PublishMode defaultMode;
    private final EnumMap<EventType, PublishMode> overrides;

    public PublishConfig(
            PublishMode defaultMode,
            Map<EventType, PublishMode> overrides
    ) {
        Objects.requireNonNull(defaultMode, "defaultMode must not be null");
        Objects.requireNonNull(overrides, "overrides must not be null");

        this.defaultMode = defaultMode;
        this.overrides = new EnumMap<>(EventType.class);
        this.overrides.putAll(overrides);
    }

    /**
     * 获取指定事件类型的发布模式
     *
     * @param type 事件类型
     * @return 发布模式
     */
    public PublishMode get(EventType type) {
        Objects.requireNonNull(type, "type must not be null");
        return overrides.getOrDefault(type, defaultMode);
    }

    /**
     * 创建默认可靠模式的发布配置
     *
     * @return 发布配置
     */
    public static PublishConfig allReliable() {
        return new PublishConfig(PublishMode.RELIABLE, Collections.emptyMap());
    }

    /**
     * 创建默认不可靠模式的发布配置
     *
     * @return 发布配置
     */
    public static PublishConfig allUnreliable() {
        return new PublishConfig(PublishMode.UNRELIABLE, Collections.emptyMap());
    }

    /**
     * 创建可靠模式并指定覆盖规则的发布配置
     *
     * @param overrides 事件类型覆盖规则
     * @return 发布配置
     */
    public static PublishConfig allReliable(Map<EventType, PublishMode> overrides) {
        return new PublishConfig(PublishMode.RELIABLE, overrides);
    }

    /**
     * 创建不可靠模式并指定覆盖规则的发布配置
     *
     * @param overrides 事件类型覆盖规则
     * @return 发布配置
     */
    public static PublishConfig allUnreliable(Map<EventType, PublishMode> overrides) {
        return new PublishConfig(PublishMode.UNRELIABLE, overrides);
    }

    public PublishMode getDefaultMode() {
        return defaultMode;
    }
}
