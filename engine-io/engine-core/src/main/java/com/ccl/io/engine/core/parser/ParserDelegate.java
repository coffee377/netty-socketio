package com.ccl.io.engine.core.parser;

import com.ccl.io.engine.Parser;
import com.ccl.io.engine.codec.VersionedEngineIODecoder;
import com.ccl.io.engine.codec.VersionedEngineIOEncoder;
import com.ccl.io.engine.protocol.EngineIOPacket;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 委托模式 Parser 实现
 *
 * <p>通过 {@link ServiceLoader} 自动发现并加载所有 {@link Parser} 实现，
 * 根据协议版本动态路由到对应的解析器</p>
 *
 * <p>工作流程：
 * <ol>
 *   <li>构造时通过 SPI 加载所有 Parser 实现</li>
 *   <li>调用时根据指定版本或数据包内容选择对应 Parser</li>
 * </ol>
 * </p>
 *
 * <p>默认使用 V4 版本，当无法确定版本时回退到 V4</p>
 *
 * @author coffee377
 * @see ServiceLoader
 * @since 4.0.0
 */
public class ParserDelegate implements Parser, VersionedEngineIODecoder, VersionedEngineIOEncoder {
    /**
     * 版本号到 Parser 实例的映射
     */
    private final Map<Integer, Parser> versionedParsers = new ConcurrentHashMap<>();
    private final List<Parser> parsers = new CopyOnWriteArrayList<>();
    /**
     * 默认版本解析器（V4）
     */
    private final Parser defaultParser;

    public ParserDelegate(Parser defaultParser) {
        this.defaultParser = defaultParser;
        // 加载所有 Parser SPI 实现
        ServiceLoader<Parser> serviceLoader = ServiceLoader.load(Parser.class);
        for (Parser parser : serviceLoader) {
            parsers.add(parser);
        }
    }

    /**
     * 构造函数
     *
     * <p>加载所有可用的 Parser 实现并注册版本映射</p>
     */
    public ParserDelegate() {
        this(Parser.NOOP);
    }

    private Parser getParser(int protocolVersion) {
        return versionedParsers.computeIfAbsent(protocolVersion,
                version -> parsers.stream()
                        .filter(parser -> parser.isSupport(protocolVersion))
                        .findFirst().orElse(defaultParser));
    }

    @Override
    public EngineIOPacket<?> decodePacket(Object data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EngineIOPacket<?> decodePacket(Object data, int protocolVersion) {
        return getParser(protocolVersion).decodePacket(data);
    }

    @Override
    public List<EngineIOPacket<?>> decodePayload(Object payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EngineIOPacket<?>> decodePayload(Object payload, int protocolVersion) {
        return getParser(protocolVersion).decodePayload(payload);
    }

    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary, int protocolVersion) {
        return getParser(protocolVersion).encodePacket(packet, supportBinary);
    }

    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary, int protocolVersion) {
        return getParser(protocolVersion).encodePayload(packets, supportBinary);
    }
}
