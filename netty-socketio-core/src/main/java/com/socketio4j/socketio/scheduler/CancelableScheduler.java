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

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;

/**
 * 可取消的调度器接口
 *
 * <p>提供定时任务的调度、取消和 ChannelHandlerContext 更新功能
 */
public interface CancelableScheduler {

    /**
     * 更新 ChannelHandlerContext
     *
     * @param ctx Netty 通道处理器上下文
     */
    void update(ChannelHandlerContext ctx);

    /**
     * 取消指定键的调度任务
     *
     * @param key 调度任务键
     */
    void cancel(SchedulerKey key);

    /**
     * 在 ChannelHandlerContext 的事件循环中调度回调任务
     *
     * @param key      调度任务键
     * @param runnable 待执行的任务
     * @param delay    延迟时间
     * @param unit     时间单位
     */
    void scheduleCallback(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit);

    /**
     * 调度一个任务
     *
     * @param runnable 待执行的任务
     * @param delay    延迟时间
     * @param unit     时间单位
     */
    void schedule(Runnable runnable, long delay, TimeUnit unit);

    /**
     * 调度一个可取消的任务
     *
     * @param key      调度任务键
     * @param runnable 待执行的任务
     * @param delay    延迟时间
     * @param unit     时间单位
     */
    void schedule(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit);

    /**
     * 关闭调度器，停止所有任务
     */
    void shutdown();

}
