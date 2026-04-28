package com.ccl.engineio.core.parser;

import com.ccl.engineio.core.protocol.DataType;
import com.ccl.engineio.core.protocol.EngineIOPacket;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Engine.IO 协议解析器接口
 *
 * <p>定义 Engine.IO 协议的编解码操作，支持不同版本的协议实现</p>
 *
 * @author coffee377
 * @see <a href="https://socket.io/zh-CN/docs/v4/engine-io-protocol/">Engine.IO 协议文档</a>
 */
public interface Parser {

    /**
     * 获取协议版本号
     *
     * @return 协议版本号（如 3、4）
     */
    int getProtocolVersion();

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

    /**
     * 解码单个数据包
     *
     * @param data     原始数据（字符串或字节数组）
     * @param dataType 数据类型
     * @return 解码后的数据包，data 为 null 时返回 null
     */
    EngineIOPacket<?> decodePacket(Object data, DataType dataType);

    /**
     * 解码多个数据包（Payload）
     *
     * @param data     原始数据
     * @param dataType 数据类型
     * @return 解码后的数据包列表
     */
    List<EngineIOPacket<?>> decodePayload(Object data, DataType dataType);

}
