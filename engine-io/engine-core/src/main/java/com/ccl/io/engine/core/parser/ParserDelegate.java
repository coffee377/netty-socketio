package com.ccl.io.engine.core.parser;

import com.ccl.io.engine.Parser;
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
public class ParserDelegate implements Parser {
    /**
     * 版本号到 Parser 实例的映射
     */
    private final Map<Integer, Parser> versionedParsers = new ConcurrentHashMap<>();
    private final List<Parser> parsers = new CopyOnWriteArrayList<>();
    /**
     * 默认版本解析器
     */
    private final Parser defaultParser;

    /**
     * 构造函数，指定默认解析器
     *
     * <p>加载所有可用的 Parser 实现并注册版本映射</p>
     *
     * @param defaultParser 默认使用的解析器
     */
    public ParserDelegate(Parser defaultParser) {
        this.defaultParser = defaultParser;
        // 加载所有 Parser SPI 实现
        ServiceLoader<Parser> serviceLoader = ServiceLoader.load(Parser.class);
        for (Parser parser : serviceLoader) {
            parsers.add(parser);
        }
    }

    /**
     * 默认构造函数
     *
     * <p>使用 ParserV4 作为默认解析器</p>
     */
    public ParserDelegate() {
        this(new ParserV4());
    }

    /**
     * 根据协议版本获取对应的 Parser，首次获取后将结果缓存
     *
     * <p>查找顺序：
     * <ol>
     *   <li>优先从缓存中返回已匹配的 Parser</li>
     *   <li>遍历所有已加载的 Parser，返回首个支持该版本的实现</li>
     *   <li>未匹配时返回默认 Parser</li>
     * </ol>
     *
     * @param protocolVersion 协议版本号
     * @return 匹配的 Parser 实例
     */
    private Parser getParser(int protocolVersion) {
        return versionedParsers.computeIfAbsent(protocolVersion,
                version -> parsers.stream()
                        .filter(parser -> parser.isSupport(protocolVersion))
                        .findFirst().orElse(defaultParser));
    }

    @Override
    public EngineIOPacket<?> decodePacket(Object data) {
        return defaultParser.decodePacket(data);
    }

    @Override
    public EngineIOPacket<?> decodePacket(Object data, int protocolVersion) {
        return getParser(protocolVersion).decodePacket(data);
    }

    @Override
    public List<EngineIOPacket<?>> decodePayload(Object payload) {
        return defaultParser.decodePayload(payload);
    }

    @Override
    public List<EngineIOPacket<?>> decodePayload(Object payload, int protocolVersion) {
        return getParser(protocolVersion).decodePayload(payload);
    }

    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary) {
        return defaultParser.encodePacket(packet, supportBinary);
    }

    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary, int protocolVersion) {
        return getParser(protocolVersion).encodePacket(packet, supportBinary);
    }

    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary) {
        return defaultParser.encodePayload(packets, supportBinary);
    }

    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary, int protocolVersion) {
        return getParser(protocolVersion).encodePayload(packets, supportBinary);
    }

}
