package com.ccl.socketio.netty.handler.codec;

import com.ccl.engineio.core.codec.EngineIOEncoder;
import com.ccl.engineio.core.codec.impl.EngineIOEncoderV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.engineio.netty.transport.PollingTransport;
import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.codec.impl.SocketIOEncoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.core.protocol.data.Event;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Socket.IO 编解码处理器
 *
 * <p>继承 Netty 的 MessageToMessageCodec，实现 Socket.IO 数据包的编解码：
 * <ul>
 *   <li>解码：将 SocketPacket 转换为 EngineIOPacket 并传递给下游</li>
 *   <li>编码：将 SocketPacket 编码为 EngineIOPacket，通过 PollingTransport 发送</li>
 * </ul>
 * </p>
 *
 * <p>当 SocketPacket 包含二进制附件时，会拆分为多个 EngineIOPacket：
 * <ul>
 *   <li>协议头 + JSON payload（第一个包）</li>
 *   <li>二进制附件（后续包）</li>
 * </ul>
 * </p>
 *
 * @author coffee377
 * @see com.ccl.socketio.core.codec.SocketEncoder
 * @see com.ccl.socketio.core.codec.SocketDecoder
 * @since 4.0.0-alpha.0
 */
@Sharable
public class SocketIOCodec extends MessageToMessageCodec<SocketPacket<?>, SocketPacket<?>> {

    /**
     * 日志记录器
     */
    private final static Logger log = LoggerFactory.getLogger(SocketIOCodec.class);

    /**
     * Socket.IO 编码器
     */
    private final SocketEncoder socketEncoder;

    /**
     * Engine.IO 编码器，用于将 Socket.IO 数据包编码为 Engine.IO 传输格式
     */
    private final EngineIOEncoder engineEncoder;

    /**
     * 默认构造函数
     */
    public SocketIOCodec() {
        this.socketEncoder = new SocketIOEncoderV5();
        this.engineEncoder = new EngineIOEncoderV4();
    }

    /**
     * 编码 Socket.IO 数据包为 Engine.IO 传输格式
     *
     * <p>将 SocketPacket 编码为 EngineIO 数据包列表并通过 PollingTransport 发送。
     * 如果数据包包含二进制附件，则拆分为多个 EngineIO 数据包（协议头 + 二进制附件）。</p>
     *
     * @param ctx Channel 上下文
     * @param msg 待编码的 Socket.IO 数据包
     * @param out 输出列表（当前未直接使用，改为通过 PollingTransport 发送）
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, SocketPacket<?> msg, List<Object> out) throws Exception {
        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        String ps = socketEncoder.encode(msg);
        if (log.isDebugEnabled()) {
            log.debug("OUT[{}] {} {}", msg.getType(), ps, sid);
        }

        List<EngineIOPacket<?>> packets = new ArrayList<>();
        EngineIOPacket<String> p1 = EngineIOPacket.builder().data(ps).build();
        packets.add(p1);

        if (msg.hasAttachments()) {
            for (byte[] attachment : msg.getAttachments()) {
                EngineIOPacket<byte[]> ap = EngineIOPacket.builder().data(attachment).build();
                packets.add(ap);
            }
        }

        ByteBuffer byteBuffer = engineEncoder.encodePayload(packets, false);
        int remaining = byteBuffer.remaining();
        byte[] bytes = new byte[remaining];
        byteBuffer.get(bytes);

        ByteBuf content = ctx.alloc().buffer();
        content.writeBytes(bytes);

        ChannelHandlerContext context = ctx.pipeline().context(PollingTransport.class);
        if (context != null) {
            PollingTransport polling = (PollingTransport) context.handler();
            sid = context.channel().attr(ChannelAttributes.SESSION_ID).get();
            polling.sendMessage(sid, content);
        }
    }

    /**
     * 解码 Socket.IO 数据包
     *
     * <p>处理接收到的 SocketPacket，根据类型执行对应逻辑：
     * <ul>
     *   <li>CONNECT：自动回复携带 sid 的连接确认包</li>
     *   <li>BINARY_EVENT：处理二进制事件（当前为临时实现）</li>
     * </ul>
     * </p>
     *
     * @param ctx Channel 上下文
     * @param msg 接收到的 Socket.IO 数据包
     * @param out 输出列表
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, SocketPacket<?> msg, List<Object> out) throws Exception {
        SocketPacket.Type type = msg.getType();
        if (log.isDebugEnabled()) {
            String s = socketEncoder.encode(msg);
            log.debug("IN[{}] {}", type, s);
        }

        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();

        if (type == SocketPacket.Type.CONNECT) {
            HashMap<String, Object> data = new HashMap<String, Object>() {{
                put("sid", sid);
            }};
            SocketPacket<HashMap<String, Object>> packet = SocketPacket.builder()
                    .type(SocketPacket.Type.CONNECT)
                    .data(data).build();
            ctx.channel().writeAndFlush(packet);
            return;
        }

        if (SocketPacket.Type.BINARY_EVENT.equals(type)) {
            // TODO: 2026/05/09 15:02 二进制事件发布不了
            Object data = msg.getData();
            byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);

            Event event = new Event();
            event.setName("pong");
            event.setArgs(Collections.singletonList(bytes));
            // 51-["pong",{"_placeholder":true,"num":0}]
            // 451-["pong",{"_placeholder":true,"num":0}]bZGF0YQ==
            SocketPacket<?> ev = SocketPacket.builder()
                    .type(SocketPacket.Type.BINARY_EVENT)
                    .attachmentsCount(1)
                    .data(event).build();
            ev.addAttachment(bytes);
            ctx.channel().writeAndFlush(ev);
        }
    }
}
