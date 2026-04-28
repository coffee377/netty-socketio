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

    private static final ParserV4 INSTANCE = new ParserV4();

    public static ParserV4 getInstance() {
        return INSTANCE;
    }

    public ParserV4() {
    }

    @Override
    public int getProtocolVersion() {
        return EngineVersion.V4.getValue();
    }

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
