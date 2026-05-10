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

import java.util.List;

import com.socketio4j.socketio.AckRequest;
import com.socketio4j.socketio.SocketIOClient;


/**
 * 全局事件监听器，捕获所有事件而无需指定具体事件名称
 */
@FunctionalInterface
public interface CatchAllEventListener {

    /**
     * 任何事件到达时调用
     *
     * @param client    客户端
     * @param event     事件名称
     * @param args      事件参数列表
     * @param ackRequest ACK 请求
     */
    void onEvent(SocketIOClient client, String event, List<Object> args, AckRequest ackRequest);
}
