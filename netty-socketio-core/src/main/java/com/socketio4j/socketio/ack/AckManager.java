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
package com.socketio4j.socketio.ack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socketio4j.socketio.AckCallback;
import com.socketio4j.socketio.Disconnectable;
import com.socketio4j.socketio.MultiTypeAckCallback;
import com.socketio4j.socketio.MultiTypeArgs;
import com.socketio4j.socketio.SocketIOClient;
import com.socketio4j.socketio.handler.ClientHead;
import com.socketio4j.socketio.protocol.Packet;
import com.socketio4j.socketio.scheduler.CancelableScheduler;
import com.socketio4j.socketio.scheduler.SchedulerKey;
import com.socketio4j.socketio.scheduler.SchedulerKey.Type;

/**
 * ACK 管理器，负责 Socket.IO 协议中确认回执的注册、超时调度及客户端断开时的清理
 *
 * <p>每个客户端会话维护一个 AckEntry，记录待处理的 ACK 回调和序号生成器
 */
public class AckManager implements Disconnectable {

    /**
     * 每个客户端会话的 ACK 条目，包含回调映射表和序号生成器
     */
    static class AckEntry {

        final Map<Long, AckCallback<?>> ackCallbacks = new ConcurrentHashMap<>();
        final AtomicLong ackIndex = new AtomicLong(-1);

        /**
         * 注册 ACK 回调并分配唯一序号
         *
         * @param callback ACK 回调
         * @return 分配的唯一序号
         */
        public long addAckCallback(AckCallback<?> callback) {
            long index = ackIndex.incrementAndGet();
            ackCallbacks.put(index, callback);
            return index;
        }

        /**
         * 获取所有已注册的 ACK 序号
         *
         * @return ACK 序号集合
         */
        public Set<Long> getAckIndexes() {
            return ackCallbacks.keySet();
        }

        /**
         * 根据序号获取 ACK 回调
         *
         * @param index ACK 序号
         * @return ACK 回调，不存在时返回 null
         */
        public AckCallback<?> getAckCallback(long index) {
            return ackCallbacks.get(index);
        }

        /**
         * 移除并返回指定序号的 ACK 回调
         *
         * @param index ACK 序号
         * @return 被移除的 ACK 回调，不存在时返回 null
         */
        public AckCallback<?> removeCallback(long index) {
            return ackCallbacks.remove(index);
        }

        /**
         * 初始化 ACK 序号，仅在当前值为 -1 时生效
         *
         * @param index 初始序号
         */
        public void initAckIndex(long index) {
            ackIndex.compareAndSet(-1, index);
        }

    }

    private static final Logger log = LoggerFactory.getLogger(AckManager.class);

    private final ConcurrentMap<UUID, AckEntry> ackEntries = new ConcurrentHashMap<>();

    private final CancelableScheduler scheduler;

    /**
     * 构造 AckManager 实例
     *
     * @param scheduler 可取消的调度器，用于 ACK 超时调度
     */
    public AckManager(CancelableScheduler scheduler) {
        super();
        this.scheduler = scheduler;
    }

    /**
     * 初始化指定会话的 ACK 序号
     *
     * @param sessionId 客户端会话 ID
     * @param index         初始序号
     */
    public void initAckIndex(UUID sessionId, long index) {
        AckEntry ackEntry = getAckEntry(sessionId);
        ackEntry.initAckIndex(index);
    }

    /**
     * 获取或创建指定会话的 AckEntry
     *
     * @param sessionId 客户端会话 ID
     * @return AckEntry 实例
     */
    private AckEntry getAckEntry(UUID sessionId) {
        AckEntry ackEntry = ackEntries.get(sessionId);
        if (ackEntry == null) {
            ackEntry = new AckEntry();
            AckEntry oldAckEntry = ackEntries.putIfAbsent(sessionId, ackEntry);
            if (oldAckEntry != null) {
                ackEntry = oldAckEntry;
            }
        }
        return ackEntry;
    }

    /**
     * 处理收到的 ACK 响应数据包
     *
     * <p>取消对应的超时任务，查找并执行回调。对于 MultiTypeAckCallback 使用多参数模式，
     * 普通 AckCallback 仅取第一个参数
     *
     * @param client 客户端实例
     * @param packet 包含 ACK 数据的包
     */
    @SuppressWarnings("unchecked")
    public void onAck(SocketIOClient client, Packet packet) {
        AckSchedulerKey key = new AckSchedulerKey(Type.ACK_TIMEOUT, client.getSessionId(), packet.getAckId());
        scheduler.cancel(key);

        AckCallback callback = removeCallback(client.getSessionId(), packet.getAckId());
        if (callback == null) {
            return;
        }
        if (callback instanceof MultiTypeAckCallback) {
            callback.onSuccess(new MultiTypeArgs(packet.getData()));
        } else {
            Object param = null;
            List<Object> args = packet.getData();
            if (!args.isEmpty()) {
                param = args.get(0);
            }
            if (args.size() > 1) {
                log.error("Wrong ack args amount. Should be only one argument, but current amount is: {}. Ack id: {}, sessionId: {}",
                        args.size(), packet.getAckId(), client.getSessionId());
            }
            callback.onSuccess(param);
        }
    }

    /**
     * 移除并返回指定会话和序号的 ACK 回调
     *
     * @param sessionId 客户端会话 ID
     * @param index     ACK 序号
     * @return ACK 回调，不存在时返回 null
     */
    private AckCallback<?> removeCallback(UUID sessionId, long index) {
        AckEntry ackEntry = ackEntries.get(sessionId);
        // may be null if client disconnected
        // before timeout occurs
        if (ackEntry != null) {
            return ackEntry.removeCallback(index);
        }
        return null;
    }

    /**
     * 获取指定会话和序号的 ACK 回调
     *
     * @param sessionId 客户端会话 ID
     * @param index     ACK 序号
     * @return ACK 回调，不存在时返回 null
     */
    public AckCallback<?> getCallback(UUID sessionId, long index) {
        AckEntry ackEntry = getAckEntry(sessionId);
        return ackEntry.getAckCallback(index);
    }

    /**
     * 注册一个 ACK 回调并分配序号，同时启动超时调度
     *
     * @param sessionId 客户端会话 ID
     * @param callback  ACK 回调
     * @return 分配的 ACK 序号
     */
    public long registerAck(UUID sessionId, AckCallback<?> callback) {
        AckEntry ackEntry = getAckEntry(sessionId);
        ackEntry.initAckIndex(0);
        long index = ackEntry.addAckCallback(callback);

        if (log.isDebugEnabled()) {
            log.debug("AckCallback registered with id: {} for client: {}", index, sessionId);
        }

        scheduleTimeout(index, sessionId, callback);

        return index;
    }

    /**
     * 调度 ACK 超时任务
     *
     * @param index     ACK 序号
     * @param sessionId 客户端会话 ID
     * @param callback  ACK 回调，其 getTimeout 决定超时时间
     */
    private void scheduleTimeout(final long index, final UUID sessionId, AckCallback<?> callback) {
        if (callback.getTimeout() == -1) {
            return;
        }
        SchedulerKey key = new AckSchedulerKey(Type.ACK_TIMEOUT, sessionId, index);
        scheduler.scheduleCallback(key, () -> {
            AckCallback<?> cb = removeCallback(sessionId, index);
            if (cb != null) {
                cb.onTimeout();
            }
        }, callback.getTimeout(), TimeUnit.SECONDS);
    }

    /**
     * 客户端断开时清理所有待处理的 ACK 回调并取消超时任务
     *
     * @param client 断开的客户端
     */
    @Override
    public void onDisconnect(ClientHead client) {
        AckEntry e = ackEntries.remove(client.getSessionId());
        if (e == null) {
            return;
        }

        Set<Long> indexes = e.getAckIndexes();
        for (Long index : indexes) {
            AckCallback<?> callback = e.getAckCallback(index);
            if (callback != null) {
                callback.onTimeout();
            }
            SchedulerKey key = new AckSchedulerKey(Type.ACK_TIMEOUT, client.getSessionId(), index);
            scheduler.cancel(key);
        }
    }

}
