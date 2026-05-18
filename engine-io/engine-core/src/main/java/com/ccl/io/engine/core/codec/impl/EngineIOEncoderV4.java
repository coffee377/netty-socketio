package com.ccl.io.engine.core.codec.impl;

import com.ccl.io.engine.codec.Codec;
import com.ccl.io.engine.codec.EngineIOEncoder;
import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.EngineIOVersion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
 *   <li>其他类型：优先使用 {@link Codec} 序列化为 JSON 字节数组，否则调用 toString()</li>
 * </ul>
 *
 * <p>批量编码时使用 V4 协议记录分隔符（0x1E）连接各数据包</p>
 *
 * @author coffee377
 * @see EngineIOEncoder
 * @see EngineIODecoderV4
 * @since 4.0.0
 */
public class EngineIOEncoderV4 implements EngineIOEncoder {

    /**
     * 编码指定协议版本的单个数据包
     *
     * <p>根据数据类型和传输方式选择合适的编码策略：
     * <ul>
     *   <li>字符串数据：直接拼接类型字节和 UTF-8 编码的负载</li>
     *   <li>二进制数据（支持二进制）：直接拼接类型字节和原始二进制数据</li>
     *   <li>二进制数据（不支持二进制）：使用 Base64 编码，添加 'b' 前缀</li>
     *   <li>其他类型：优先使用 Codec 序列化为 JSON 字节数组，否则调用 toString()</li>
     * </ul>
     *
     * @param packet          数据包
     * @param supportBinary  是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @param protocolVersion 协议版本号
     * @return 编码后的字节数组
     */
    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary, int protocolVersion) {
        byte typeByte = packet.getType().getByte();
        byte[] typeBytes = new byte[]{typeByte};
        Object data = packet.getData();
        byte[] result;
        if (data instanceof String) {
            byte[] payloadBytes = ((String) data).getBytes(StandardCharsets.UTF_8);
            result = new byte[typeBytes.length + payloadBytes.length];
            System.arraycopy(typeBytes, 0, result, 0, typeBytes.length);
            System.arraycopy(payloadBytes, 0, result, typeBytes.length, payloadBytes.length);
            return result;
        } else if (data instanceof byte[]) {
            if (supportBinary) {
                byte[] payloadBytes = (byte[]) data;
                result = new byte[typeBytes.length + payloadBytes.length];
                System.arraycopy(typeBytes, 0, result, 0, typeBytes.length);
                System.arraycopy(payloadBytes, 0, result, typeBytes.length, payloadBytes.length);
                return result;
            } else {
                byte[] base64Bytes = Base64.getEncoder().encode((byte[]) data);
                result = new byte[1 + base64Bytes.length];
                result[0] = 'b';
                System.arraycopy(base64Bytes, 0, result, 1, base64Bytes.length);
                return result;
            }
        } else if (data != null) {
            Codec codec = getCodec();
            byte[] bytes;
            if (codec != null) {
                bytes = codec.serializeValueAsBytes(data);
            } else {
                bytes = data.toString().getBytes(StandardCharsets.UTF_8);
            }
            result = new byte[bytes.length + 1];
            result[0] = typeByte;
            System.arraycopy(bytes, 0, result, 1, bytes.length);
            return result;
        }
        return typeBytes;
    }

    /**
     * 编码指定协议版本的多个数据包为 Payload
     *
     * <p>将多个数据包序列化为单个 Payload，使用 V4 协议记录分隔符（0x1E）
     * 连接各数据包。先计算总大小以分配精确的 ByteBuffer，再逐一编码写入</p>
     *
     * @param packets         数据包列表
     * @param supportBinary  是否支持二进制（true: 直接传输二进制, false: Base64 编码）
     * @param protocolVersion 协议版本号
     * @return 编码后的字节缓冲区，空列表返回空缓冲区
     */
    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary, int protocolVersion) {
        if (packets == null || packets.isEmpty()) {
            return ByteBuffer.allocate(0);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (int i = 0; i < packets.size(); i++) {
                byte[] data = encodePacket(packets.get(i), supportBinary);
                out.write(data);
                if (i < packets.size() - 1) {
                    out.write(V4_RECORD_SEPARATOR);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ByteBuffer.wrap(out.toByteArray());
    }

    /**
     * 检查是否支持指定协议版本
     *
     * @param protocolVersion 协议版本号
     * @return 仅当协议版本为 V4 时返回 true
     */
    @Override
    public boolean isSupport(int protocolVersion) {
        return EngineIOVersion.V4.getValue() == protocolVersion;
    }

    @Override
    public Codec getCodec() {
        return new JacksonCodec();
    }
}
