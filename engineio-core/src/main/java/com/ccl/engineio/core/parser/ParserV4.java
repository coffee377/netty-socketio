package com.ccl.engineio.core.parser;

import com.ccl.engineio.core.protocol.DataType;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.EngineVersion;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Engine.IO v4 协议解析器实现
 *
 * <p>V4 协议特点：
 * <ul>
 *   <li>使用 0x1E (RS, Record Separator) 作为多数据包分隔符</li>
 *   <li>二进制数据可选择 Base64 编码（supportsBinary=false 时）</li>
 *   <li>数据包格式：类型字符 + 负载数据</li>
 * </ul>
 * </p>
 *
 * @author coffee377
 * @see Parser
 */
public class ParserV4 implements Parser {

    /**
     * V4 协议记录分隔符（0x1E）
     */
    public static final byte V4_RECORD_SEPARATOR = 0x1E;

    /**
     * 单例实例
     */
    private static final ParserV4 INSTANCE = new ParserV4();

    /**
     * 获取单例实例
     * @return ParserV4 实例
     */
    public static ParserV4 getInstance() {
        return INSTANCE;
    }

    /**
     * 默认构造函数
     */
    public ParserV4() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProtocolVersion() {
        return EngineVersion.V4.getValue();
    }

    /**
     * 编码单个数据包
     * <p>根据 supportsBinary 参数决定二进制数据的编码方式：
     * <ul>
     *   <li>true：直接输出二进制字节</li>
     *   <li>false：将二进制数据转为 Base64 编码，前缀为 'b'</li>
     * </ul>
     * </p>
     *
     * @param packet         待编码的数据包
     * @param supportsBinary 是否支持二进制传输
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
     * 编码多个数据包为 payload（用于 HTTP 长轮询）
     * <p>多个数据包之间使用 V4_RECORD_SEPARATOR（0x1E）分隔</p>
     *
     * @param packets        数据包列表
     * @param supportsBinary 是否支持二进制传输
     * @return 编码后的 ByteBuffer
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
     * 解码单个数据包
     * <p>支持从 String 或 byte[] 输入解码，底层委托给 EngineIOPacket.fromBytes</p>
     *
     * @param data     待解码的数据（String 或 byte[]）
     * @param dataType 数据类型（明文或二进制）
     * @return 解码后的数据包，输入为 null 时返回 null
     * @throws IllegalArgumentException 数据类型不支持时抛出
     */
    @Override
    public EngineIOPacket<?> decodePacket(Object data, DataType dataType) {
        if (data == null) {
            return null;
        }

        byte[] byteData;
        if (data instanceof String) {
            byteData = ((String) data).getBytes(StandardCharsets.UTF_8);
        } else if (data instanceof byte[]) {
            byteData = (byte[]) data;
        } else {
            throw new IllegalArgumentException("Invalid type for data: " + data.getClass().getName());
        }

        return EngineIOPacket.fromBytes(byteData, dataType);
    }

    /**
     * 解码 payload 中的多个数据包
     * <p>使用 V4_RECORD_SEPARATOR（0x1E）作为分隔符拆分数据</p>
     *
     * @param data     待解码的 payload 数据（String 或 byte[]）
     * @param dataType 数据类型（明文或二进制）
     * @return 解码后的数据包列表
     * @throws IllegalArgumentException 数据类型不支持时抛出
     */
    @Override
    public List<EngineIOPacket<?>> decodePayload(Object data, DataType dataType) {
        List<EngineIOPacket<?>> packets = new ArrayList<>();

        byte[] bytesData;
        if (data instanceof String) {
            bytesData = ((String) data).getBytes(StandardCharsets.UTF_8);
        } else if (data instanceof byte[]) {
            bytesData = (byte[]) data;
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
     * 按分隔符分割字节数据
     * <p>先统计分隔符数量以预分配列表容量，再进行一次遍历完成分割</p>
     *
     * @param data 原始字节数据
     * @param sep  分隔符
     * @return 分割后的字节数组列表
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
