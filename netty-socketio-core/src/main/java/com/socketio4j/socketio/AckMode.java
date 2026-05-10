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
package com.socketio4j.socketio;

/**
 * ACK 响应模式
 *
 * <p>控制服务端在收到 ACK 请求后是自动发送确认还是由开发者手动控制
 */
public enum AckMode {

    /**
     * 每次 ACK 请求时自动发送确认，跳过事件处理中的异常
     */
    AUTO,

    /**
     * 仅在事件处理成功后才自动发送确认
     */
    AUTO_SUCCESS_ONLY,

    /**
     * 关闭自动 ACK 确认，需手动调用 AckRequest.sendAckData 发送确认
     */
    MANUAL

}
