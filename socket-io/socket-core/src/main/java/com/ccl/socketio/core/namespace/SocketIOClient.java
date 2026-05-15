package com.ccl.socketio.core.namespace;

import com.ccl.io.engine.core.entity.HandshakeData;
import com.ccl.io.engine.protocol.Transport;
import com.ccl.socketio.core.operations.ClientOperations;

import java.net.SocketAddress;
import java.util.Set;

/**
 * Socket.IO 客户端接口
 *
 * <p>定义 Socket.IO 客户端的行为，包括获取连接信息、管理房间和通道状态。
 *
 * @author coffee377
 * @since 4.0.0
 */
public interface SocketIOClient extends ClientOperations {

    /**
     * 获取握手数据
     *
     * @return 握手数据对象
     */
    HandshakeData getHandshakeData();

    /**
     * 获取传输层类型
     *
     * @return 传输层类型（WebSocket 或 Polling）
     */
    Transport getTransport();

    /**
     * 获取 Engine.IO 协议版本
     *
     * @return 协议版本号
     */
    int getEngineIOVersion();

    /**
     * 获取所属命名空间
     *
     * @return 命名空间对象
     */
    SocketIONamespace getNamespace();

    /**
     * 获取会话 ID
     *
     * @return 会话 ID
     */
    String getSessionId();

    /**
     * 获取远程地址
     *
     * @return 远程 Socket 地址
     */
    SocketAddress getRemoteAddress();

    /**
     * 判断通道是否处于打开状态
     *
     * @return 通道打开时返回 true
     */
    boolean isChannelOpen();

    /**
     * 加入指定房间
     *
     * @param room 房间名称
     */
    void joinRoom(String room);

    /**
     * 加入多个房间
     *
     * @param rooms 房间名称集合
     */
    void joinRooms(Set<String> rooms);

    /**
     * 离开指定房间
     *
     * @param room 房间名称
     */
    void leaveRoom(String room);

    /**
     * 离开多个房间
     *
     * @param rooms 房间名称集合
     */
    void leaveRooms(Set<String> rooms);

    /**
     * 获取客户端已加入的所有房间
     *
     * @return 房间名称集合
     */
    Set<String> getAllRooms();

    /**
     * 获取指定房间内的成员数量
     *
     * @param room 房间名称
     * @return 房间内成员数
     */
    int getCurrentRoomSize(String room);

}
