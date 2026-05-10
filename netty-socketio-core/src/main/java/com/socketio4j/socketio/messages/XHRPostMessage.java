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
 * XHR POST 消息，表示客户端通过 HTTP POST 发送的数据
 */
public class XHRPostMessage extends HttpMessage {

    /**
     * 构造 XHRPostMessage
     *
     * @param origin    请求来源
     * @param sessionId 会话 ID
     */
    public XHRPostMessage(String origin, UUID sessionId) {
        super(origin, sessionId);
    }

}
