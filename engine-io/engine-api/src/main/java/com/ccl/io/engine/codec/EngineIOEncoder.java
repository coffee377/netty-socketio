package com.ccl.io.engine.codec;

import com.ccl.io.engine.EngineIO;
import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.EngineIOVersion;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Engine.IO 协议编码器接口
 *
 * <p>负责将 {@link EngineIOPacket} 数据包编码为可传输的字节数据，
 * 支持单个数据包编码和批量 Payload 编码两种模式</p>
 *
 * @author coffee377
 * @see EngineIODecoder
 * @since 4.0.0
 */
public interface EngineIOEncoder extends EngineIO {

    /**
     * 编码单个数据包（使用默认协议版本）
     *
     * <p>默认委托到 {@link #encodePacket(EngineIOPacket, boolean, int)}，使用 V4 版本</p>
     *
     * @param packet         数据包
     * @param supportBinary 是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @return 编码后的字节数组
     */
    default byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary) {
        return encodePacket(packet, supportBinary, EngineIOVersion.V4.getValue());
    }

    /**
     * 编码指定协议版本的单个数据包
     *
     * <p>根据是否支持二进制传输，选择直接传输或 Base64 编码方式对数据包进行序列化</p>
     *
     * @param packet          数据包
     * @param supportBinary   是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @param protocolVersion Engine.IO 协议版本号
     * @return 编码后的字节数组
     */
    byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary, int protocolVersion);

    /**
     * 编码多个数据包（Payload，使用默认协议版本）
     *
     * <p>默认委托到 {@link #encodePayload(List, boolean, int)}，使用 V4 版本</p>
     *
     * @param packets        数据包列表
     * @param supportBinary 是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @return 编码后的字节缓冲区，空列表返回空缓冲区
     */
    default ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary) {
        return encodePayload(packets, supportBinary, EngineIOVersion.V4.getValue());
    }

    /**
     * 编码指定协议版本的多个数据包为 Payload
     *
     * <p>将多个数据包序列化为单个 Payload，使用记录分隔符（如 0x1E）连接各数据包，
     * 适用于一次传输多个数据包的场景</p>
     *
     * @param packets         数据包列表
     * @param supportBinary   是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @param protocolVersion Engine.IO 协议版本号
     * @return 编码后的字节缓冲区，空列表返回空缓冲区
     */
    ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary, int protocolVersion);

}
