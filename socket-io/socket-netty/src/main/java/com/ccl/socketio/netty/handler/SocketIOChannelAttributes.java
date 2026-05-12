package com.ccl.socketio.netty.handler;

import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.util.AttributeKey;

/**
 * Socket.IO Channel 属性键定义
 *
 * <p>用于在 Channel 的 AttributeMap 中存储和检索与 Socket.IO 协议处理相关的数据。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class SocketIOChannelAttributes {

    /**
     * Socket.IO 数据包属性键
     *
     * <p>用于存储待组装附件的 Socket.IO 数据包。
     * 当解析带二进制附件的数据包时，先将包头存入此属性，
     * 后续附件到达时从该属性取出并补充。
     */
    public static final AttributeKey<SocketPacket<?>> SOCKET_PACKET = AttributeKey.valueOf("socketPacket");

    private SocketIOChannelAttributes() {
    }
}
