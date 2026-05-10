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
package com.socketio4j.socketio.scheduler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;


/**
 * 超时专用调度器（基于 Netty HashedWheelTimer）
 *
 * <p>与 HashedWheelScheduler 的区别：添加新超时任务时会取消同键的旧任务，
 * 修复了高并发场景下 cancel() 和 schedule() 非原子操作导致的问题
 */
public class HashedWheelTimeoutScheduler implements CancelableScheduler {

    private final ConcurrentMap<SchedulerKey, Timeout> scheduledFutures = new ConcurrentHashMap<>();
    private final HashedWheelTimer executorService;

    private volatile ChannelHandlerContext ctx;
    
    public HashedWheelTimeoutScheduler() {
        executorService = new HashedWheelTimer();
    }
    
    public HashedWheelTimeoutScheduler(ThreadFactory threadFactory) {
        executorService = new HashedWheelTimer(threadFactory);
    }

    @Override
    public void update(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void cancel(SchedulerKey key) {
        Timeout timeout = scheduledFutures.remove(key);
        if (timeout != null) {
            timeout.cancel();
        }
    }

    @Override
    public void schedule(final Runnable runnable, long delay, TimeUnit unit) {
        executorService.newTimeout(timeout -> runnable.run(), delay, unit);
    }

    @Override
    public void scheduleCallback(final SchedulerKey key, final Runnable runnable, long delay, TimeUnit unit) {
        Timeout timeout = executorService.newTimeout(timeout1 -> ctx.executor().execute(() -> {
            scheduledFutures.remove(key);
            runnable.run();
        }), delay, unit);

        replaceScheduledFuture(key, timeout);
    }

    @Override
    public void schedule(final SchedulerKey key, final Runnable runnable, long delay, TimeUnit unit) {
        Timeout timeout = executorService.newTimeout(timeout1 -> {
            scheduledFutures.remove(key);
            runnable.run();
        }, delay, unit);

        replaceScheduledFuture(key, timeout);
    }

    @Override
    public void shutdown() {
        executorService.stop();
    }

    private void replaceScheduledFuture(final SchedulerKey key, final Timeout newTimeout) {
        final Timeout oldTimeout;

        if (newTimeout.isExpired()) {
            // no need to put already expired timeout to scheduledFutures map.
            // simply remove old timeout
            oldTimeout = scheduledFutures.remove(key);
        } else {
            oldTimeout = scheduledFutures.put(key, newTimeout);
        }

        // if there was old timeout, cancel it
        if (oldTimeout != null) {
            oldTimeout.cancel();
        }
    }
}
