package com.ccl.engineio.core.codec.impl;

import com.ccl.engineio.core.codec.EngineIOEncoder;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.EngineVersion;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Engine.IO V4 协议编码器实现
 *
 * <p>实现 {@link EngineIOEncoder} 接口，负责 V4 版本协议的数据编码。
 * 支持以下编码方式：
 * <ul>
 *   <li>字符串数据：直接拼接类型字节和 UTF-8 编码的负载数据</li>
 *   <li>二进制数据：支持二进制时直接拼接，否则使用 Base64 编码（前缀 'b'）</li>
 *   <li>其他类型：转换为字符串后拼接</li>
 * </ul>
 *
 * <p>批量编码时使用 V4 协议记录分隔符（0x1E）连接各数据包</p>
 *
 * @see EngineIOEncoder
 * @see EngineIODecoderV4
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class EngineIOEncoderV4 implements EngineIOEncoder {

    /**
     * 编码单个数据包
     *
     * <p>根据数据类型和传输方式选择合适的编码策略：
     * <ul>
     *   <li>字符串数据：直接拼接类型字节和 UTF-8 编码的负载</li>
     *   <li>二进制数据（支持二进制）：直接拼接类型字节和原始二进制数据</li>
     *   <li>二进制数据（不支持二进制）：使用 Base64 编码，添加 'b' 前缀</li>
     *   <li>其他类型：调用 toString() 后拼接</li>
     * </ul>
     *
     * @param packet         数据包
     * @param supportsBinary 是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @return 编码后的字节数组
     */
    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportsBinary) {
        byte[] typeBytes = new byte[]{packet.getType().getByte()};
        Object data = packet.getData();

        if (data instanceof String) {
            byte[] payloadBytes = ((String) data).getBytes(StandardCharsets.UTF_8);
            byte[] result = new byte[typeBytes.length + payloadBytes.length];
            System.arraycopy(typeBytes, 0, result, 0, typeBytes.length);
            System.arraycopy(payloadBytes, 0, result, typeBytes.length, payloadBytes.length);
            return result;
        } else if (data instanceof byte[]) {
            if (supportsBinary) {
                byte[] payloadBytes = (byte[]) data;
                byte[] result = new byte[typeBytes.length + payloadBytes.length];
                System.arraycopy(typeBytes, 0, result, 0, typeBytes.length);
                System.arraycopy(payloadBytes, 0, result, typeBytes.length, payloadBytes.length);
                return result;
            } else {
                String base64 = Base64.getEncoder().encodeToString((byte[]) data);
                byte[] base64Bytes = base64.getBytes(StandardCharsets.UTF_8);
                byte[] result = new byte[1 + base64Bytes.length];
                result[0] = 'b';
                System.arraycopy(base64Bytes, 0, result, 1, base64Bytes.length);
                return result;
            }
        } else {
            byte[] result = typeBytes;
            if (data != null) {
                String json = data.toString();
                result = new byte[typeBytes.length + json.length()];
                System.arraycopy(typeBytes, 0, result, 0, typeBytes.length);
                System.arraycopy(json.getBytes(StandardCharsets.UTF_8), 0, result, typeBytes.length, json.length());
            }
            return result;
        }
    }

    /**
     * 编码多个数据包（Payload）
     *
     * <p>将多个数据包序列化为单个 Payload，使用 V4 协议记录分隔符（0x1E）
     * 连接各数据包。先计算总大小以分配精确的 ByteBuffer，再逐一编码写入</p>
     *
     * @param packets        数据包列表
     * @param supportsBinary 是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @return 编码后的字节缓冲区，空列表返回空缓冲区
     */
    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportsBinary) {
        if (packets == null || packets.isEmpty()) {
            return ByteBuffer.allocate(0);
        }

        int totalSize = 0;
        for (int i = 0; i < packets.size(); i++) {
            byte[] encoded = encodePacket(packets.get(i), supportsBinary);
            totalSize += encoded.length;
            if (i < packets.size() - 1) {
                totalSize += 1;
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        for (int i = 0; i < packets.size(); i++) {
            byte[] encoded = encodePacket(packets.get(i), supportsBinary);
            buffer.put(encoded);
            if (i < packets.size() - 1) {
                buffer.put(V4_RECORD_SEPARATOR);
            }
        }
        buffer.flip();
        return buffer;
    }

    /**
     * 检查是否支持指定协议版本
     *
     * @param protocolVersion 协议版本号
     * @return 仅当协议版本为 V4 时返回 true
     */
    @Override
    public boolean isSupport(int protocolVersion) {
        return EngineVersion.V4.getValue() == protocolVersion;
    }
}
