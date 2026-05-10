package com.ccl.socketio.core.listener;

import com.ccl.socketio.core.protocol.SocketPacket;

/**
 * Socket.IO 数据包监听器
 *
 * <p>当从 Engine.IO 层接收到数据包时回调此接口，
 * 允许对入站数据包进行转换或增强处理。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public interface PacketListener {

    /**
     * 创建或转换数据包
     *
     * @param packet 原始入站数据包
     * @param sid    Session ID
     * @return 处理后的数据包
     */
    SocketPacket<?> createPacket(SocketPacket<?> packet, String sid);

}
