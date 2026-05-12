package com.ccl.socketio.core.namespace.impl;

import com.ccl.io.engine.core.entity.HandshakeData;
import com.ccl.io.engine.protocol.Transport;
import com.ccl.socketio.core.namespace.SocketIOClient;
import com.ccl.socketio.core.namespace.SocketIONamespace;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.core.protocol.data.Event;

import java.net.SocketAddress;
import java.util.Set;

/**
 * Socket.IO 客户端默认实现
 *
 * <p>封装客户端在特定命名空间下的会话信息，提供发送事件、管理房间等操作能力。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class NamespaceClient implements SocketIOClient {
    private final SocketIONamespace namespace;
    private final String sid;

    /**
     * 创建命名空间客户端实例
     *
     * @param namespace 所属命名空间
     * @param sid       会话 ID
     */
    public NamespaceClient(SocketIONamespace namespace, String sid) {
        this.namespace = namespace;
        this.sid = sid;
        namespace.addClient(this);
    }

    @Override
    public HandshakeData getHandshakeData() {
        return null;
    }

    @Override
    public Transport getTransport() {
        return null;
    }

    @Override
    public int getEngineIOVersion() {
        return 0;
    }

    @Override
    public SocketIONamespace getNamespace() {
        return namespace;
    }

    @Override
    public String getSessionId() {
        return sid;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public boolean isChannelOpen() {
        return false;
    }

    @Override
    public void joinRoom(String room) {
//        namespace.joinRoom(room, getSessionId());
    }

    @Override
    public void joinRooms(Set<String> rooms) {

    }

    @Override
    public void leaveRoom(String room) {

    }

    @Override
    public void leaveRooms(Set<String> rooms) {

    }

    @Override
    public Set<String> getAllRooms() {
        return null;
    }

    @Override
    public int getCurrentRoomSize(String room) {
        return 0;
    }

    @Override
    public <T> void send(SocketPacket<T> packet) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void sendEvent(Event event) {

    }
}
