package com.ccl.socketio.core.codec;

import com.ccl.socketio.core.protocol.SocketPacket;

/**
 * Socket.IO 协议解码器接口
 *
 * <p>负责将 Socket.IO 协议的字符串格式数据包解析为 {@link SocketPacket} 实例</p>
 *
 * @author coffee377
 * @see SocketEncoder
 * @since 4.0.0-alpha.0
 */
public interface SocketDecoder extends SocketIO {

    /**
     * 解码 Socket.IO 数据包
     *
     * <p>Socket.IO 协议数据包格式：
     * <pre>[packet type][# of binary attachments-][namespace,][acknowledgment id][JSON payload]</pre>
     * </p>
     *
     * @param raw 原始字符串数据
     * @return 解析后的数据包实例，raw 为空时返回 null
     */
    SocketPacket<?> decode(String raw);

    /**
     * 解码 Socket.IO 数据包为指定类型
     *
     * @param raw   原始字符串数据
     * @param clazz 目标数据类型
     * @param <T>   目标类型
     * @return 解析后的数据包实例，raw 为空时返回 null
     */
    <T> SocketPacket<T> decode(String raw, Class<T> clazz);

}
