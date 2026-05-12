package com.ccl.socketio.core.codec;

import com.ccl.io.engine.codec.Codec;

/**
 * Socket.IO 编解码器基础接口
 *
 * <p>为所有 Socket.IO 编解码器提供公共常量和协议版本支持检查，
 * 所有 {@link SocketDecoder} 和 {@link SocketEncoder} 实现应继承此接口</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 * @see SocketDecoder
 * @see SocketEncoder
 */
public interface SocketIO {

    /**
     * 检查当前编解码器是否支持指定协议版本
     *
     * @param protocolVersion 协议版本号
     * @return 如果支持该版本则返回 true，否则返回 false
     */
    default boolean isSupport(int protocolVersion) {
        return true;
    }

    /**
     * 获取字符串编解码器
     *
     * @return 字符串编解码器实例
     */
    Codec getStringCodec();

}
