/**
 * Copyright (c) 2025 The Socketio4j Project
 * Parent project : Copyright (c) 2012-2025 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.socketio4j.socketio.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer 指标工厂，基于用户提供的 MeterRegistry 创建 MicrometerSocketIOMetrics 实例
 *
 * <p>该工厂不创建 MeterRegistry 或 Exporter，由上层应用负责选择和配置。
 * socketio4j 仅依赖 Micrometer Core API，与具体后端解耦
 *
 * @since 4.0.0
 */

public final class MicrometerMetricsFactory {

    private static final Logger log = LoggerFactory.getLogger(MicrometerMetricsFactory.class);

    private MicrometerMetricsFactory() {
    }

    /**
     * 使用指定 MeterRegistry 创建 Micrometer 指标实现
     *
     * @param registry         MeterRegistry
     * @param histogramEnabled 是否启用百分位直方图
     * @return MicrometerSocketIOMetrics 实例
     */
    public static MicrometerSocketIOMetrics using(MeterRegistry registry, boolean histogramEnabled) {
        return new MicrometerSocketIOMetrics(registry, histogramEnabled);
    }
}