package com.ccl.socketio.core.codec;

import com.ccl.socketio.core.protocol.SocketPacket;

/**
 * Socket.IO 协议编码器接口
 *
 * <p>负责将 {@link SocketPacket} 实例编码为 Socket.IO 协议的字符串格式</p>
 *
 * @see SocketDecoder
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface SocketEncoder {

    /**
     * 编码 Socket.IO 数据包
     *
     * <p>Socket.IO 协议数据包格式：
     * <pre>[packet type][# of binary attachments-][namespace,][acknowledgment id][JSON payload]</pre>
     * </p>
     *
     * @param packet 数据包实例
     * @return 编码后的字符串
     */
    String encode(SocketPacket<?> packet);

}
