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

import com.socketio4j.socketio.Transport;
import com.socketio4j.socketio.handler.ClientHead;

/**
 * 出站数据包消息，携带待发送的 ClientHead 和传输方式信息
 *
 * <p>由 ClientHead.send() 创建，EncoderHandler 据此将数据包写入相应传输通道
 */
public class OutPacketMessage extends HttpMessage {

    private final ClientHead clientHead;
    private final Transport transport;

    /**
     * 构造 OutPacketMessage
     *
     * @param clientHead 客户端头部
     * @param transport  传输方式
     */
    public OutPacketMessage(ClientHead clientHead, Transport transport) {
        super(clientHead.getOrigin(), clientHead.getSessionId());

        this.clientHead = clientHead;
        this.transport = transport;
    }

    /**
     * 获取传输方式
     *
     * @return Transport
     */
    public Transport getTransport() {
        return transport;
    }

    /**
     * 获取客户端头部
     *
     * @return ClientHead
     */
    public ClientHead getClientHead() {
        return clientHead;
    }

}
