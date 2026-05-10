package com.ccl.engineio.core.parser;

import com.ccl.engineio.core.codec.EngineIODecoder;
import com.ccl.engineio.core.codec.EngineIOEncoder;
import com.ccl.engineio.core.codec.impl.EngineIODecoderV4;
import com.ccl.engineio.core.codec.impl.EngineIOEncoderV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.EngineVersion;

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
     * 单例实例
     */
    private static final ParserV4 INSTANCE = new ParserV4();

    /**
     * 获取单例实例
     *
     * @return ParserV4 实例
     */
    public static ParserV4 getInstance() {
        return INSTANCE;
    }

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
     */
    public ParserV4() {
        this.encoder = new EngineIOEncoderV4();
        this.decoder = new EngineIODecoderV4();
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
        return encoder.encodePacket(packet, supportsBinary);
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
        return encoder.encodePayload(packets, supportsBinary);
    }

    /**
     * 解码单个数据包
     * <p>支持从 String 或 byte[] 输入解码，底层委托给 EngineIOPacket.fromBytes</p>
     *
     * @param data     待解码的数据（String 或 byte[]）
     * @return 解码后的数据包，输入为 null 时返回 null
     * @throws IllegalArgumentException 数据类型不支持时抛出
     */
    @Override
    public EngineIOPacket<?> decodePacket(Object data) {
        return decoder.decodePacket(data);
    }

    /**
     * 解码 payload 中的多个数据包
     * <p>使用 V4_RECORD_SEPARATOR（0x1E）作为分隔符拆分数据</p>
     *
     * @param data     待解码的 payload 数据（String 或 byte[]）
     * @return 解码后的数据包列表
     * @throws IllegalArgumentException 数据类型不支持时抛出
     */
    @Override
    public List<EngineIOPacket<?>> decodePayload(Object data) {
        return decoder.decodePayload(data);
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
