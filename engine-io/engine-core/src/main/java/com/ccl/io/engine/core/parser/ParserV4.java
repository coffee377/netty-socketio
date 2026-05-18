package com.ccl.io.engine.core.parser;

import com.ccl.io.engine.Parser;
import com.ccl.io.engine.protocol.EngineIOVersion;
import com.ccl.io.engine.codec.EngineIODecoder;
import com.ccl.io.engine.codec.EngineIOEncoder;
import com.ccl.io.engine.core.codec.impl.EngineIODecoderV4;
import com.ccl.io.engine.core.codec.impl.EngineIOEncoderV4;
import com.ccl.io.engine.protocol.EngineIOPacket;

import java.nio.ByteBuffer;
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
     * Engine.IO 解码器
     */
    private final EngineIODecoder decoder;

    /**
     * Engine.IO 编码器
     */
    private final EngineIOEncoder encoder;

    /**
     * 默认构造函数
     *
     * <p>初始化 V4 版本的编码器和解码器</p>
     */
    public ParserV4() {
        this.encoder = new EngineIOEncoderV4();
        this.decoder = new EngineIODecoderV4();
    }

    @Override
    public EngineIOPacket<?> decodePacket(Object data) {
        return decodePacket(data, EngineIOVersion.V4.getValue());
    }

    @Override
    public EngineIOPacket<?> decodePacket(Object data, int protocolVersion) {
        return decoder.decodePacket(data, protocolVersion);
    }

    @Override
    public List<EngineIOPacket<?>> decodePayload(Object payload) {
        return decodePayload(payload, EngineIOVersion.V4.getValue());
    }

    @Override
    public List<EngineIOPacket<?>> decodePayload(Object data, int protocolVersion) {
        return decoder.decodePayload(data, protocolVersion);
    }

    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary) {
        return encodePacket(packet, supportBinary, EngineIOVersion.V4.getValue());
    }

    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary, int protocolVersion) {
        return encoder.encodePacket(packet, supportBinary, protocolVersion);
    }

    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary) {
        return encodePayload(packets, supportBinary, EngineIOVersion.V4.getValue());
    }

    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary, int protocolVersion) {
        return encoder.encodePayload(packets, supportBinary, protocolVersion);
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

}
