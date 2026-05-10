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

import com.socketio4j.socketio.SocketIOServer;

/**
 * 服务器停止前监听器，在 Socket.IO 服务器即将停止前回调
 */
public interface ServerBeforeStopListener {

    /**
     * 服务器停止前调用
     *
     * @param server SocketIOServer 实例
     */
    void beforeStop(SocketIOServer server);
}