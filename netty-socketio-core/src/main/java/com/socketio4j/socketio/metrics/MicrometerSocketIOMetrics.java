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
package com.socketio4j.socketio.metrics;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;


/**
 * 基于 Micrometer 的 SocketIOMetrics 实现，记录事件、ACK、连接、房间和计时等指标
 *
 * <p>按命名空间管理指标，使用 Micrometer Counter、Gauge、Timer 等类型。
 * 支持百分位直方图和百分位摘要两种统计模式
 */
public final class MicrometerSocketIOMetrics implements SocketIOMetrics {

    private final MeterRegistry registry;
    private final boolean histogramEnabled;
    private static final XXHash64 XX_HASH =
            XXHashFactory.fastestInstance().hash64();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    /**
     * 将字符串哈希为 long 值，用于未知事件名称的基数估计
     *
     * @param key 待哈希的字符串
     * @return 64 位哈希值
     */
    private static long hashToLong(String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        return XX_HASH.hash(bytes, 0, bytes.length, 0);
    }

    /* ===================== Constructors ===================== */

    /** 默认构造，使用百分位摘要模式（单 JVM 友好） */
    public MicrometerSocketIOMetrics(MeterRegistry registry) {
        this(registry, false);
    }

    /**
     * 构造 MicrometerSocketIOMetrics
     *
     * @param registry         MeterRegistry
     * @param histogramEnabled true 启用百分位直方图（推荐集群环境），false 使用百分位摘要
     */
    public MicrometerSocketIOMetrics(MeterRegistry registry, boolean histogramEnabled) {
        Objects.requireNonNull(registry, "registry can not be null");
        this.registry = registry;
        this.histogramEnabled = histogramEnabled;
    }

    /**
     * 获取 MeterRegistry
     *
     * @return MeterRegistry
     */
    public MeterRegistry getRegistry() {
        return registry;
    }

    /* ===================== Helpers ===================== */

    private final ConcurrentMap<String, NamespaceMeters> namespaces = new ConcurrentHashMap<>();

    /**
     * 获取或创建指定命名空间的指标容器
     *
     * @param namespace 命名空间
     * @return NamespaceMeters
     */
    private NamespaceMeters ns(String namespace) {
        return namespaces.computeIfAbsent(
                namespace,
                n -> new NamespaceMeters(registry, n, histogramEnabled)
        );
    }


    /* ===================== Events ===================== */

    @Override
    public void eventReceived(String ns) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        ns(ns).getEventReceived().increment();
    }

    @Override
    public void eventHandled(String ns, long durationNanos) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        NamespaceMeters m = ns(ns);
        if (durationNanos > 0) {
            m.getEventProcessing().record(durationNanos, TimeUnit.NANOSECONDS);
        }
        m.getEventHandled().increment();
    }

    @Override
    public void eventFailed(String ns) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        ns(ns).getEventFailed().increment();
    }

    @Override
    public void eventSent(String ns, int recipients) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        NamespaceMeters m = ns(ns);

        if (recipients > 0) {
            m.getEventSent().increment(recipients);
        }
    }

    @Override
    public void unknownEventReceived(String ns) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        ns(ns).getEventUnknown().increment();
    }

    @Override
    public void unknownEventNames(String ns, String eventName) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        if (eventName == null) {
            return;
        }
        final String finalNs = ns;
        executorService.execute(() -> {
            ns(finalNs).recordUnknownEvent(hashToLong(finalNs + ":" + eventName));

        });
    }

    /* ===================== ACK ===================== */

    @Override
    public void ackSent(String ns, long latencyNanos) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        NamespaceMeters m = ns(ns);
        if (latencyNanos > 0) {
            m.getAckLatency().record(latencyNanos, TimeUnit.NANOSECONDS);
        }
        m.getAckSent().increment();
    }

    @Override
    public void ackMissing(String ns) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        ns(ns).getAckMissing().increment();
    }

    /* ===================== Connections ===================== */

    @Override
    public void connect(String ns) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        NamespaceMeters m = ns(ns);
        m.getConnect().increment();
        m.getConnected().incrementAndGet();
    }

    @Override
    public void disconnect(String ns) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        NamespaceMeters m = ns(ns);
        m.getDisconnect().increment();
        m.getConnected().decrementAndGet();
    }

    /* ===================== Rooms ===================== */

    @Override
    public void roomJoin(String ns) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        NamespaceMeters m = ns(ns);
        m.getRoomJoin().increment();
        m.getRoomMembers().incrementAndGet();
    }

    @Override
    public void roomLeave(String ns) {
        if (ns.isEmpty()) {
            ns = "default";
        }
        NamespaceMeters m = ns(ns);
        m.getRoomLeave().increment();
        m.getRoomMembers().getAndUpdate(v -> Math.max(0, v - 1));
    }

    /**
     * 获取 MeterRegistry
     *
     * @return MeterRegistry
     */
    public MeterRegistry registry() {
        return  registry;
    }

    /**
     * 关闭并释放执行器资源
     */
    public void close() {
        executorService.shutdown();
    }


}
