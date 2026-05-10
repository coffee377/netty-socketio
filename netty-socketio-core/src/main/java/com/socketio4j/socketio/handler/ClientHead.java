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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.DisconnectableHub;
import com.socketio4j.socketio.HandshakeData;
import com.socketio4j.socketio.Transport;
import com.socketio4j.socketio.ack.AckManager;
import com.socketio4j.socketio.messages.OutPacketMessage;
import com.socketio4j.socketio.namespace.Namespace;
import com.socketio4j.socketio.protocol.EngineIOVersion;
import com.socketio4j.socketio.protocol.Packet;
import com.socketio4j.socketio.protocol.PacketType;
import com.socketio4j.socketio.scheduler.CancelableScheduler;
import com.socketio4j.socketio.scheduler.SchedulerKey;
import com.socketio4j.socketio.scheduler.SchedulerKey.Type;
import com.socketio4j.socketio.store.Store;
import com.socketio4j.socketio.store.StoreFactory;
import com.socketio4j.socketio.transport.NamespaceClient;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AttributeKey;

/**
 * 客户端头部，管理单个 Socket.IO 连接的会话状态
 *
 * <p>维护会话的传输通道（WebSocket/Polling）、命名空间客户端列表、握手数据、
 * ACK 管理器及心跳调度。支持传输层升级和数据包发送
 */
public class ClientHead {

    private static final Logger log = LoggerFactory.getLogger(ClientHead.class);

    /**
     * Channel 属性键，用于绑定 ClientHead 实例
     */
    public static final AttributeKey<ClientHead> CLIENT = AttributeKey.<ClientHead>valueOf("client");

    private final AtomicBoolean disconnected = new AtomicBoolean();
    private final Map<Namespace, NamespaceClient> namespaceClients = new ConcurrentHashMap<>();
    private final Map<Transport, TransportState> channels = new HashMap<Transport, TransportState>(2);
    private final HandshakeData handshakeData;
    private final UUID sessionId;

    private final EngineIOVersion engineIOVersion;

    private final Store store;
    private final DisconnectableHub disconnectableHub;
    private final AckManager ackManager;
    private ClientsBox clientsBox;
    private final CancelableScheduler scheduler;
    private final Configuration configuration;

    private Packet lastBinaryPacket;

    // TODO use lazy set
    private volatile Transport currentTransport;

    /**
     * 构造 ClientHead 实例
     *
     * @param sessionId        会话 ID
     * @param ackManager       ACK 管理器
     * @param disconnectable   可断开组件集线器
     * @param storeFactory     存储工厂
     * @param handshakeData    握手数据
     * @param clientsBox       客户端容器
     * @param transport        初始传输方式
     * @param scheduler        可取消的调度器
     * @param configuration    配置
     * @param params           请求参数，包含 Engine.IO 版本信息
     */
    public ClientHead(UUID sessionId, AckManager ackManager, DisconnectableHub disconnectable,
                      StoreFactory storeFactory, HandshakeData handshakeData, ClientsBox clientsBox, Transport transport, CancelableScheduler scheduler,
                      Configuration configuration, Map<String, List<String>> params) {
        this.sessionId = sessionId;
        this.ackManager = ackManager;
        this.disconnectableHub = disconnectable;
        this.store = storeFactory.createStore(sessionId);
        this.handshakeData = handshakeData;
        this.clientsBox = clientsBox;
        this.currentTransport = transport;
        this.scheduler = scheduler;
        this.configuration = configuration;

        channels.put(Transport.POLLING, new TransportState());
        channels.put(Transport.WEBSOCKET, new TransportState());

        List<String> versions = params.getOrDefault(EngineIOVersion.EIO, new ArrayList<>());
        if (versions.isEmpty()) {
            engineIOVersion = EngineIOVersion.UNKNOWN;
        } else {
            engineIOVersion = EngineIOVersion.fromValue(versions.get(0));
        }
    }

    /**
     * 将 Channel 绑定到指定传输方式
     *
     * <p>替换同一传输方式的旧 Channel，并将当前客户端注册到 ClientsBox
     *
     * @param channel   Netty Channel
     * @param transport 传输方式
     */
    public void bindChannel(Channel channel, Transport transport) {
        log.debug("binding channel: {} to transport: {}", channel, transport);

        TransportState state = channels.get(transport);
        Channel prevChannel = state.update(channel);
        if (prevChannel != null) {
            clientsBox.remove(prevChannel);
        }
        clientsBox.add(channel, this);

        sendPackets(transport, channel);
    }

    /**
     * 释放 Polling 传输的 Channel
     *
     * @param channel 要释放的 Channel
     */
    public void releasePollingChannel(Channel channel) {
        try {
            TransportState state = channels.get(Transport.POLLING);
            if (channel.equals(state.getChannel())) {
                clientsBox.remove(channel);
                state.update(null);
            }
        } catch (Exception e) {
            log.error("Failed to release polling channel for session: {}", sessionId, e);
        }
    }

    /**
     * 获取请求来源 Origin
     *
     * @return Origin 头信息
     */
    public String getOrigin() {
        return handshakeData.getHttpHeaders().get(HttpHeaderNames.ORIGIN);
    }

    /**
     * 通过当前传输方式发送数据包
     *
     * @param packet 数据包
     * @return ChannelFuture，若无法立即发送则返回 null
     */
    public ChannelFuture send(Packet packet) {
        return send(packet, getCurrentTransport());
    }

    /**
     * 取消 Ping 定时任务
     */
    public void cancelPing() {
        try {
            SchedulerKey key = new SchedulerKey(Type.PING, sessionId);
            scheduler.cancel(key);
        } catch (Exception e) {
            log.error("Failed to cancel ping task for session: {}", sessionId, e);
        }
    }

    /**
     * 取消 Ping 超时定时任务
     */
    public void cancelPingTimeout() {
        try {
            SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, sessionId);
            scheduler.cancel(key);
        } catch (Exception e) {
            log.error("Failed to cancel ping timeout task for session: {}", sessionId, e);
        }
    }

    /**
     * 调度 Ping 发送任务
     *
     * <p>仅对 Engine.IO V4 协议发送 PING 包
     */
    public void schedulePing() {
        cancelPing();
        final SchedulerKey key = new SchedulerKey(Type.PING, sessionId);
        scheduler.schedule(key, () -> {
            ClientHead client = clientsBox.get(sessionId);
            if (client != null) {
                EngineIOVersion version = client.getEngineIOVersion();
                //only send ping packet for engine.io version 4
                if (EngineIOVersion.V4.equals(version)) {
                    client.send(new Packet(PacketType.PING, version));
                }
                schedulePing();
            }
        }, configuration.getPingInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * 调度 Ping 超时检测任务
     *
     * <p>超时后自动断开客户端连接
     */
    public void schedulePingTimeout() {
        cancelPingTimeout();
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, sessionId);
        scheduler.schedule(key, () -> {
            ClientHead client = clientsBox.get(sessionId);
            if (client != null) {
                client.disconnect();
                log.debug("{} removed due to ping timeout", sessionId);
            }
        }, configuration.getPingTimeout() + configuration.getPingInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * 通过指定传输方式发送数据包
     *
     * @param packet    数据包
     * @param transport 传输方式
     * @return ChannelFuture，若无法立即发送则返回 null
     */
    public ChannelFuture send(Packet packet, Transport transport) {
        TransportState state = channels.get(transport);
        state.getPacketsQueue().add(packet);

        Channel channel = state.getChannel();
        if (channel == null
                || (transport == Transport.POLLING && channel.attr(EncoderHandler.WRITE_ONCE).get() != null)) {
            return null;
        }
        return sendPackets(transport, channel);
    }

    /**
     * 将队列中的数据包写入 Channel
     *
     * @param transport 传输方式
     * @param channel   Netty Channel
     * @return ChannelFuture
     */
    private ChannelFuture sendPackets(Transport transport, Channel channel) {
        return channel.writeAndFlush(new OutPacketMessage(this, transport));
    }

    /**
     * 移除命名空间客户端，若所有命名空间客户端均已移除则触发断开
     *
     * @param client 命名空间客户端
     */
    public void removeNamespaceClient(NamespaceClient client) {
        namespaceClients.remove(client.getNamespace());
        if (namespaceClients.isEmpty()) {
            disconnectableHub.onDisconnect(this);
        }
    }

    /**
     * 获取指定命名空间的子客户端
     *
     * @param namespace 命名空间
     * @return NamespaceClient，未连接时返回 null
     */
    public NamespaceClient getChildClient(Namespace namespace) {
        return namespaceClients.get(namespace);
    }

    /**
     * 添加命名空间客户端
     *
     * @param namespace 命名空间
     * @return 新建的 NamespaceClient
     */
    public NamespaceClient addNamespaceClient(Namespace namespace) {
        NamespaceClient client = new NamespaceClient(this, namespace);
        namespaceClients.put(namespace, client);
        return client;
    }

    /**
     * 获取所有已连接的命名空间
     *
     * @return 命名空间集合
     */
    public Set<Namespace> getNamespaces() {
        return namespaceClients.keySet();
    }

    /**
     * 检查客户端是否仍处于连接状态
     *
     * @return 连接中返回 true
     */
    public boolean isConnected() {
        return !disconnected.get();
    }

    /**
     * 处理 Channel 断开事件
     *
     * <p>取消所有定时任务，通知所有命名空间客户端断开，清理 Channel 映射
     */
    public void onChannelDisconnect() {
        cancelPing();
        cancelPingTimeout();

        disconnected.set(true);
        for (NamespaceClient client : namespaceClients.values()) {
            client.onDisconnect();
        }
        for (TransportState state : channels.values()) {
            if (state.getChannel() != null) {
                clientsBox.remove(state.getChannel());
            }
        }
    }

    /**
     * 获取握手数据
     *
     * @return HandshakeData
     */
    public HandshakeData getHandshakeData() {
        return handshakeData;
    }

    /**
     * 获取 ACK 管理器
     *
     * @return AckManager
     */
    public AckManager getAckManager() {
        return ackManager;
    }

    /**
     * 获取会话 ID
     *
     * @return UUID
     */
    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * 获取远程地址
     *
     * @return SocketAddress
     */
    public SocketAddress getRemoteAddress() {
        return handshakeData.getAddress();
    }

    /**
     * 主动断开客户端连接
     *
     * <p>发送 DISCONNECT 数据包后关闭 Channel
     */
    public void disconnect() {
        Packet packet = new Packet(PacketType.MESSAGE, engineIOVersion);
        packet.setSubType(PacketType.DISCONNECT);
        ChannelFuture future = send(packet);
        if (future != null) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

        onChannelDisconnect();
    }

    /**
     * 检查是否有任一传输通道处于打开状态
     *
     * @return 至少一个 Channel 激活时返回 true
     */
    public boolean isChannelOpen() {
        for (TransportState state : channels.values()) {
            if (state.getChannel() != null
                    && state.getChannel().isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取会话存储
     *
     * @return Store 实例
     */
    public Store getStore() {
        return store;
    }

    /**
     * 判断指定 Channel 是否属于指定传输方式
     *
     * @param channel   Netty Channel
     * @param transport 传输方式
     * @return 匹配返回 true
     */
    public boolean isTransportChannel(Channel channel, Transport transport) {
        TransportState state = channels.get(transport);
        if (state.getChannel() == null) {
            return false;
        }
        return state.getChannel().equals(channel);
    }

    /**
     * 升级当前传输方式
     *
     * <p>将另一传输方式的数据包队列合并到新传输方式，完成切换
     *
     * @param currentTransport 新的传输方式
     */
    public void upgradeCurrentTransport(Transport currentTransport) {
        TransportState state = channels.get(currentTransport);

        for (Entry<Transport, TransportState> entry : channels.entrySet()) {
            if (!entry.getKey().equals(currentTransport)) {

                Queue<Packet> queue = entry.getValue().getPacketsQueue();
                state.setPacketsQueue(queue);

                sendPackets(currentTransport, state.getChannel());
                this.currentTransport = currentTransport;
                log.debug("Transport upgraded to: {} for: {}", currentTransport, sessionId);
                break;
            }
        }
    }

    /**
     * 获取当前传输方式
     *
     * @return Transport
     */
    public Transport getCurrentTransport() {
        return currentTransport;
    }

    /**
     * 获取指定传输方式的数据包队列
     *
     * @param transport 传输方式
     * @return 数据包队列
     */
    public Queue<Packet> getPacketsQueue(Transport transport) {
        return channels.get(transport).getPacketsQueue();
    }

    /**
     * 设置最后一个二进制数据包
     *
     * @param lastBinaryPacket 二进制数据包
     */
    public void setLastBinaryPacket(Packet lastBinaryPacket) {
        this.lastBinaryPacket = lastBinaryPacket;
    }

    /**
     * 获取最后一个二进制数据包
     *
     * @return 二进制数据包
     */
    public Packet getLastBinaryPacket() {
        return lastBinaryPacket;
    }

    /**
     * 获取 Engine.IO 协议版本
     *
     * @return EngineIOVersion
     */
    public EngineIOVersion getEngineIOVersion() {
        return engineIOVersion;
    }

    /**
     * 检查当前传输方式的 Channel 是否可写
     *
     * @return 可写返回 true
     */
    public boolean isWritable() {
        TransportState state = channels.get(getCurrentTransport());
        Channel channel = state.getChannel();
        return channel != null && channel.isWritable();
    }


}
