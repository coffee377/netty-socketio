package com.ccl.io.engine.netty.handler.codec;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.codec.EngineIODecoder;
import com.ccl.io.engine.core.codec.impl.EngineIODecoderV4;
import com.ccl.io.engine.listener.EngineListener;
import com.ccl.io.engine.message.EngineMessagePack;
import com.ccl.io.engine.protocol.EngineIOPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Engine.IO 入站数据包解码处理器
 *
 * <p>将 {@link EngineMessagePack} 解码为 {@link EngineIOPacket}，并处理协议级控制消息。
 * 此类是 Engine.IO 协议解析流水线中的关键环节，位于传输层与协议层之间。
 * </p>
 *
 * <p><b>职责说明：</b>
 * <ul>
 *   <li>接收来自底层传输的原始 {@link EngineMessagePack}</li>
 *   <li>使用 {@link EngineIODecoder} 将二进制数据解码为 Engine.IOPacket 对象</li>
 *   <li>对 MESSAGE 类型数据包，传递给下游 {@link com.ccl.socketio.netty.handler.codec.SocketPacketDecoder} 处理</li>
 *   <li>对 CLOSE、PING、PONG、UPGRADE、NOOP 等控制消息，直接在内部处理</li>
 * </ul>
 * </p>
 *
 * <p><b>线程安全：</b>
 * 此类标注为 {@link Sharable}，可在多个 Channel 共享同一实例。
 * 解码操作均为无状态计算，线程安全。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0
 * @see EngineIODecoder
 * @see EngineIOPacket
 */
@Sharable
public class EnginePacketDecoder extends MessageToMessageDecoder<EngineMessagePack> {

    private static final Logger log = LoggerFactory.getLogger(EnginePacketDecoder.class);

    /**
     * Engine.IO payload 解码器
     *
     * <p>负责将字节数组解析为 Engine.IOPacket 对象列表。
     * 支持多种协议版本，通过构造函数注入具体实现。</p>
     */
    private final EngineIODecoder decoder;

    /**
     * 协议级控制消息监听器
     *
     * <p>处理 Engine.IO 协议规范中定义的非数据消息：
     * <ul>
     *   <li>{@link EngineIOPacket.Type#CLOSE} - 连接关闭，触发客户端断开</li>
     *   <li>{@link EngineIOPacket.Type#PING}/{@link EngineIOPacket.Type#PONG} - 心跳保活</li>
     *   <li>{@link EngineIOPacket.Type#UPGRADE} - 协议升级确认</li>
     *   <li>{@link EngineIOPacket.Type#NOOP} - 空操作，用于保活探测</li>
     * </ul>
     * </p>
     *
     * <p><b>注意：</b>MESSAGE 类型数据包不在此处理，而是添加到输出列表传递给下游处理器。</p>
     */
    private final EngineListener listener = (packet, client, transport) -> {
        EngineIOPacket.Type type = packet.getType();
        switch (type) {
            case CLOSE:
                client.disconnect();
                break;
            case PING:
            case PONG:
            case UPGRADE:
            case NOOP:
                log.debug("Engine.IO control message handled: {}", type);
                break;
            default:
                log.error("Unknown Engine.IO packet type: {}", type);
        }
    };

    /**
     * 使用指定的解码器构造解码处理器
     *
     * @param decoder Engine.IO payload 解码器，非 null
     */
    public EnginePacketDecoder(EngineIODecoder decoder) {
        this.decoder = decoder;
    }

    /**
     * 使用默认 V4 版本解码器构造解码处理器
     *
     * <p>默认使用 {@link EngineIODecoderV4}，适用于大多数场景。</p>
     */
    public EnginePacketDecoder() {
        this(new EngineIODecoderV4());
    }

    /**
     * 解码 EngineMessage 为 EngineIOPacket
     *
     * <p>核心解码流程：
     * <ol>
     *   <li>从消息中提取字节内容和关联的客户端信息</li>
     *   <li>将 ByteBuf 中的数据写入字节数组输出流</li>
     *   <li>调用解码器解析 payload 为数据包列表</li>
     *   <li>遍历数据包，根据类型分别处理或向下游传递</li>
     * </ol>
     * </p>
     *
     * <p><b>输出规则：</b>
     * <ul>
     *   <li>MESSAGE 类型数据包 → 添加到输出列表，传递给 {@link com.ccl.socketio.netty.handler.codec.SocketPacketDecoder}</li>
     *   <li>其他控制类型 → 由 listener 内部处理，不向下游传递</li>
     * </ul>
     * </p>
     *
     * @param ctx Netty 通道上下文，用于日志记录和通道操作
     * @param msg 来自下游 Handler 的 EngineMessage，包含字节内容和客户端引用
     * @param out 解码后的数据包输出列表，将被下游 Handler 消费
     * @throws Exception 解码过程中可能抛出的异常
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, EngineMessagePack msg, List<Object> out) throws Exception {
        ByteBuf content = msg.getContent();
        EngineClient client = msg.getClient();

        if (log.isTraceEnabled()) {
            log.trace("IN message: {} for sessionId: {}", content.toString(CharsetUtil.UTF_8), client.getSessionId());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        content.readBytes(bos, content.readableBytes());
        List<EngineIOPacket<?>> packets = decoder.decodePayload(bos.toByteArray());

        for (EngineIOPacket<?> packet : packets) {
            if (log.isDebugEnabled()) {
                log.debug("IN [{}] {}", packet.getType(), packet);
            }

            if (EngineIOPacket.Type.MESSAGE.equals(packet.getType())) {
                // MESSAGE 类型数据包向下游传递，由 SocketPacketDecoder 处理
                // SocketPacketDecoder 通过 Channel Attribute 缓存未完成的分包，
                // 只有附件全部到达后才输出完整数据包，借此保证消息顺序
                out.add(packet);
            } else {
                listener.onPacket(packet, client, msg.getTransport());
            }
        }

    }

}
