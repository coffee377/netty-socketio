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
package com.socketio4j.socketio.messages;

import java.util.UUID;

/**
 * HTTP 消息基类，包含请求来源和会话 ID
 *
 * <p>所有 EncoderHandler 编码的消息类型均继承此类
 */
public abstract class HttpMessage {

    private final String origin;
    private final UUID sessionId;

    /**
     * 构造 HttpMessage
     *
     * @param origin    请求来源
     * @param sessionId 会话 ID
     */
    public HttpMessage(String origin, UUID sessionId) {
        this.origin = origin;
        this.sessionId = sessionId;
    }

    /**
     * 获取请求来源
     *
     * @return origin
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * 获取会话 ID
     *
     * @return UUID
     */
    public UUID getSessionId() {
        return sessionId;
    }

}
