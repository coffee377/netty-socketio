package com.ccl.engineio.core.codec.impl;

import com.ccl.engineio.core.codec.Decoder;
import com.ccl.engineio.core.protocol.DataType;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.EngineVersion;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Engine.IO V4 协议解码器实现
 *
 * <p>实现 {@link Decoder} 接口，负责 V4 版本协议的数据解码。
 * 支持以下解码规则：
 * <ul>
 *   <li>'b' 前缀：Base64 编码的二进制数据（作为 MESSAGE 类型）</li>
 *   <li>类型字节 + 数据：数据包类型 + 负载数据</li>
 *   <li>其他：整个字节数组作为 MESSAGE 类型数据</li>
 * </ul>
 *
 * <p>批量解码时使用 V4 协议记录分隔符（0x1E）分割各数据包</p>
 *
 * @see Decoder
 * @see EngineV4Encoder
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class EngineV4Decoder implements Decoder {

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

    /**
     * 解码单个数据包
     *
     * <p>支持 String 和 byte[] 两种输入类型，
     * String 会转换为 UTF-8 字节数组进行处理</p>
     *
     * @param data     原始数据（字符串或字节数组）
     * @param dataType 数据类型（文本或二进制）
     * @return 解码后的数据包，data 为 null 时返回 null
     * @throws IllegalArgumentException 当输入类型不支持时
     */
    @Override
    public EngineIOPacket<?> decodePacket(Object data, DataType dataType) {
        if (data == null) return null;
        byte[] bytesData;
        if (data instanceof String) {
            bytesData = ((String) data).getBytes(StandardCharsets.UTF_8);
        } else if (data instanceof byte[]) {
            bytesData = (byte[]) data;
        } else {
            throw new IllegalArgumentException("Invalid type for data: " + data.getClass().getName());
        }
        return fromBytes(bytesData, dataType);
    }

    /**
     * 解码多个数据包（Payload）
     *
     * <p>先将原始数据转换为字节数组，使用 V4 协议记录分隔符（0x1E）
     * 分割各数据包，然后逐一解码</p>
     *
     * @param payload  原始数据（字符串或字节数组）
     * @param dataType 数据类型（文本或二进制）
     * @return 解码后的数据包列表，空数据返回空列表
     * @throws IllegalArgumentException 当输入类型不支持时
     */
    @Override
    public List<EngineIOPacket<?>> decodePayload(Object payload, DataType dataType) {
        List<EngineIOPacket<?>> packets = new ArrayList<>();

        byte[] bytesData;
        if (payload instanceof String) {
            bytesData = ((String) payload).getBytes(StandardCharsets.UTF_8);
        } else if (payload instanceof byte[]) {
            bytesData = (byte[]) payload;
        } else {
            throw new IllegalArgumentException("data must be a string or byte[]");
        }

        List<byte[]> splitPackets = splitData(bytesData, V4_RECORD_SEPARATOR);
        for (byte[] packetData : splitPackets) {
            if (packetData.length > 0) {
                EngineIOPacket<?> packet = decodePacket(packetData, dataType);
                if (packet == null) {
                    continue;
                }
                packets.add(packet);
            }
        }

        return packets;
    }

    /**
     * 将字节数组解析为 {@link EngineIOPacket} 数据包
     *
     * <p>解析规则：
     * <ul>
     *   <li>'b' 前缀：Base64 解码为二进制数据</li>
     *   <li>类型字节在 OPEN(0x30) 到 NOOP(0x36) 范围内：提取类型和负载</li>
     *   <li>其他情况：整个数组作为 MESSAGE 类型的负载</li>
     * </ul>
     *
     * @param byteData 原始字节数据
     * @param dataType 数据类型（文本或二进制）
     * @return 解析后的数据包实例
     */
    public EngineIOPacket<?> fromBytes(byte[] byteData, DataType dataType) {
        EngineIOPacket.Builder<?> builder = EngineIOPacket.builder();
        if (byteData.length == 0) {
            return builder.build();
        }

        int firstByte = byteData[0];

        if (firstByte == 'b') {
            byte[] data = Base64.getDecoder().decode(new String(byteData, 1, byteData.length - 1));
            return builder.data(data).build();
        }

        if (firstByte >= EngineIOPacket.Type.OPEN.getStringByte() && firstByte <= EngineIOPacket.Type.NOOP.getStringByte()) {
            EngineIOPacket.Type type = EngineIOPacket.Type.fromByte((byte) firstByte);
            if (byteData.length > 1) {
                byte[] data = new byte[byteData.length - 1];
                System.arraycopy(byteData, 1, data, 0, data.length);
                return builder.type(type).data(data).build();
            }
            return builder.type(type).build();
        }

        return builder.data(byteData).build();
    }

    /**
     * 按指定分隔符拆分字节数组
     *
     * <p>使用指定的分隔符（如 0x1E）将字节数组拆分为多个片段，
     * 跳过空片段（连续分隔符或尾部分隔符）</p>
     *
     * @param data 待拆分的字节数组
     * @param sep  分隔符字节值
     * @return 拆分后的字节数组列表（不含空片段）
     */
    private List<byte[]> splitData(byte[] data, byte sep) {
        List<byte[]> result = new ArrayList<>();

        int count = 0;
        for (byte b : data) {
            if (b == sep) {
                count++;
            }
        }

        result = new ArrayList<>(count + 1);
        int start = 0;

        for (int i = 0; i < data.length; i++) {
            if (data[i] == sep) {
                if (i > start) {
                    byte[] chunk = new byte[i - start];
                    System.arraycopy(data, start, chunk, 0, i - start);
                    result.add(chunk);
                }
                start = i + 1;
            }
        }

        if (start < data.length) {
            byte[] chunk = new byte[data.length - start];
            System.arraycopy(data, start, chunk, 0, data.length - start);
            result.add(chunk);
        }

        return result;
    }
}
