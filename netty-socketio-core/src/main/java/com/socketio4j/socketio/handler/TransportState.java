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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.socketio4j.socketio.protocol.Packet;

import io.netty.channel.Channel;

/**
 * 传输状态，维护某个传输方式（WebSocket 或 Polling）的 Channel 和数据包队列
 */
public class TransportState {

    private Queue<Packet> packetsQueue = new ConcurrentLinkedQueue<>();
    private Channel channel;

    /**
     * 设置数据包队列
     *
     * @param packetsQueue 数据包队列
     */
    public void setPacketsQueue(Queue<Packet> packetsQueue) {
        this.packetsQueue = packetsQueue;
    }

    /**
     * 获取数据包队列
     *
     * @return 数据包队列
     */
    public Queue<Packet> getPacketsQueue() {
        return packetsQueue;
    }

    /**
     * 获取绑定的 Channel
     *
     * @return Channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * 更新绑定的 Channel 并返回旧的 Channel
     *
     * @param channel 新 Channel
     * @return 旧 Channel
     */
    public Channel update(Channel channel) {
        Channel prevChannel = this.channel;
        this.channel = channel;
        return prevChannel;
    }

}
