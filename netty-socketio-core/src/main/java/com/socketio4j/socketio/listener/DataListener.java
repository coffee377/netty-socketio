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

import com.socketio4j.socketio.AckRequest;
import com.socketio4j.socketio.SocketIOClient;

/**
 * 数据事件监听器，处理客户端发来的普通事件数据
 *
 * @param <T> 数据类型
 */
public interface DataListener<T> {

    /**
     * 收到客户端数据时调用
     *
     * @param client    接收数据的客户端
     * @param data      收到的数据对象
     * @param ackSender ACK 请求
     */
    void onData(SocketIOClient client, T data, AckRequest ackSender) throws Exception;

}
