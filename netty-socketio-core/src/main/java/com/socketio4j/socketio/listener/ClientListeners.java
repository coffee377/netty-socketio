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
package com.socketio4j.socketio.listener;


/**
 * 客户端事件监听器注册接口，定义事件、连接、断开、拦截等回调的注册方法
 */
public interface ClientListeners {

    /**
     * 注册多类型事件监听器
     *
     * @param eventName  事件名称
     * @param listener   多类型事件监听器
     * @param eventClass 事件数据类型
     */
    void addMultiTypeEventListener(String eventName, MultiTypeEventListener listener, Class<?>... eventClass);

    /**
     * 注册事件监听器
     *
     * @param eventName  事件名称
     * @param eventClass 事件数据类型
     * @param listener   数据监听器
     * @param <T>        数据类型
     */
    <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener);

    /**
     * 注册事件拦截器
     *
     * @param eventInterceptor 事件拦截器
     */
    void addEventInterceptor(EventInterceptor eventInterceptor);

    /**
     * 注册断开监听器
     *
     * @param listener 断开监听器
     */
    void addDisconnectListener(DisconnectListener listener);

    /**
     * 注册连接监听器
     *
     * @param listener 连接监听器
     */
    void addConnectListener(ConnectListener listener);

    /**
     * 注册 Ping 监听器
     *
     * @deprecated 使用 {@link #addPongListener(PongListener)} 替代
     * @param listener Ping 监听器
     */
    @Deprecated
    void addPingListener(PingListener listener);

    /**
     * 注册 Pong 监听器
     *
     * @param listener Pong 监听器
     */
    void addPongListener(PongListener listener);

    /**
     * 注册监听器对象（通过注解识别事件方法）
     *
     * @param listeners 监听器对象
     */
    void addListeners(Object listeners);

    /**
     * 批量注册监听器
     *
     * @param listeners 监听器可迭代对象
     * @param <L>       监听器类型
     */
    <L> void addListeners(Iterable<L> listeners);

    /**
     * 注册监听器对象并指定监听器类
     *
     * @param listeners      监听器对象
     * @param listenersClass 监听器类
     */
    void addListeners(Object listeners, Class<?> listenersClass);

    /**
     * 移除指定事件名称的所有监听器
     *
     * @param eventName 事件名称
     */
    void removeAllListeners(String eventName);

    /**
     * 添加全局事件监听器（捕获所有事件）
     *
     * @param listener 全局事件监听器
     */
    default void addOnAnyEventListener(CatchAllEventListener listener) {
        throw new UnsupportedOperationException("addOnAnyEventListener is not implemented");
    }

    /**
     * 移除全局事件监听器
     *
     * @param listener 全局事件监听器
     */
    default void removeOnAnyEventListener(CatchAllEventListener listener) {
        throw new UnsupportedOperationException("removeOnAnyEventListener is not implemented");
    }

    /**
     * 注册全局事件监听器
     *
     * @param listener 全局事件监听器
     */
    default void onAny(CatchAllEventListener listener) {
        addOnAnyEventListener(listener);
    }

    /**
     * 取消注册全局事件监听器
     *
     * @param listener 全局事件监听器
     */
    default void offAny(CatchAllEventListener listener) {
        removeOnAnyEventListener(listener);
    }

}
