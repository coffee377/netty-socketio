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

/**
 * Socket.IO 指标接口，定义事件、ACK、连接、房间等维度的度量方法
 *
 * <p>提供静态 noop() 工厂方法返回空实现
 */
public interface SocketIOMetrics {

    /* ===================== Events ===================== */

    /**
     * 记录收到事件
     *
     * @param namespace 命名空间
     */
    void eventReceived(String namespace);

    /**
     * 记录事件处理完成及耗时
     *
     * @param namespace    命名空间
     * @param durationNanos 处理耗时（纳秒）
     */
    void eventHandled(String namespace, long durationNanos);

    /**
     * 记录事件处理失败
     *
     * @param namespace 命名空间
     */
    void eventFailed(String namespace);

    /**
     * 记录事件发送
     *
     * @param namespace  命名空间
     * @param recipients 接收者数量
     */
    void eventSent(String namespace, int recipients);

    /**
     * 记录收到未知事件
     *
     * @param namespace 命名空间
     */
    void unknownEventReceived(String namespace);

    /**
     * 记录未知事件名称
     *
     * @param namespace 命名空间
     * @param eventName 事件名称
     */
    void unknownEventNames(String namespace, String eventName);

    /* ===================== ACK ===================== */

    /**
     * 记录 ACK 已发送及延迟
     *
     * @param namespace    命名空间
     * @param latencyNanos ACK 延迟（纳秒）
     */
    void ackSent(String namespace, long latencyNanos);

    /**
     * 记录 ACK 丢失
     *
     * @param namespace 命名空间
     */
    void ackMissing(String namespace);

    /* ===================== Connections ===================== */

    /**
     * 记录连接
     *
     * @param namespace 命名空间
     */
    void connect(String namespace);

    /**
     * 记录断开
     *
     * @param namespace 命名空间
     */
    void disconnect(String namespace);

    /* ===================== Rooms ===================== */

    /**
     * 记录加入房间
     *
     * @param namespace 命名空间
     */
    void roomJoin(String namespace);

    /**
     * 记录离开房间
     *
     * @param namespace 命名空间
     */
    void roomLeave(String namespace);

    /* ===================== Factory ===================== */

    /**
     * 获取空实现的指标实例
     *
     * @return NoopSocketIOMetrics 实例
     */
    static SocketIOMetrics noop() {
        return NoopSocketIOMetrics.INSTANCE;
    }
}
