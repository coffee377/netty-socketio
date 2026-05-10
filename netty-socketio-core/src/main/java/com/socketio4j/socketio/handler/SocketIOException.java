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
package com.socketio4j.socketio.handler;

/**
 * Socket.IO 运行时异常
 *
 * <p>封装 Socket.IO 处理过程中发生的异常，继承 RuntimeException
 */
public class SocketIOException extends RuntimeException {

    private static final long serialVersionUID = -9218908839842557188L;

    /**
     * 构造 SocketIOException
     *
     * @param message 异常信息
     * @param cause   原始异常
     */
    public SocketIOException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造 SocketIOException
     *
     * @param message 异常信息
     */
    public SocketIOException(String message) {
        super(message);
    }

    /**
     * 构造 SocketIOException
     *
     * @param cause 原始异常
     */
    public SocketIOException(Throwable cause) {
        super(cause);
    }

}
