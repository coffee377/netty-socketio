package com.ccl.engineio.core.codec;

import com.ccl.engineio.core.protocol.EngineIOPacket;

import java.nio.ByteBuffer;
import java.util.List;

public interface Encoder {

    /**
     * 编码单个数据包
     *
     * @param packet         数据包
     * @param supportsBinary 是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @return 编码后的字节数组
     */
    byte[] encodePacket(EngineIOPacket<?> packet, boolean supportsBinary);

    /**
     * 编码多个数据包（Payload）
     *
     * @param packets        数据包列表
     * @param supportsBinary 是否支持二进制
     * @return 编码后的字节缓冲区
     */
    ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportsBinary);
}
