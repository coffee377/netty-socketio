package com.ccl.socketio.netty.handler.codec;

import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import com.ccl.engineio.netty.transport.PollingTransport;
import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.codec.impl.SocketIOEncoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @since 4.0.0-alpha.0
 * @see com.ccl.socketio.core.codec.SocketEncoder
 * @see com.ccl.socketio.core.codec.SocketDecoder
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
    private final SocketEncoder encoder;

    /**
     * 默认构造函数
     */
    public SocketIOCodec() {
        this.encoder = new SocketIOEncoderV5();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SocketPacket<?> msg, List<Object> out) throws Exception {
        String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        List<EngineIOPacket<?>> packets = new ArrayList<>();
        String ps = encoder.encode(msg);
        EngineIOPacket<String> p1 = EngineIOPacket.builder().data(ps).build();
        packets.add(p1);

        if (log.isDebugEnabled()) {
            log.debug("OUT[{}] {} {}", msg.getType(), ps, sid);
        }
        if (msg.hasAttachments()) {
            log.debug("hasAttachments {}", msg.getAttachments());
            for (byte[] attachment : msg.getAttachments()) {
                EngineIOPacket<byte[]> ap = EngineIOPacket.builder().data(attachment).build();
                packets.add(ap);
            }
            log.debug("ps {}", ps);
        }
        ChannelHandlerContext context = ctx.pipeline().context(PollingTransport.class);
        if (context != null) {
            PollingTransport polling = (PollingTransport) context.handler();
            sid = context.channel().attr(ChannelAttributes.SESSION_ID).get();
            polling.sendMessage(sid, packets);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, SocketPacket<?> msg, List<Object> out) throws Exception {
        SocketPacket.Type type = msg.getType();
        if (log.isDebugEnabled()) {
            String s = encoder.encode(msg);
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
            List<?> list = Arrays.asList("pong", "[{\"_placeholder\":true,\"num\":0}]");
            SocketPacket<?> ev = SocketPacket.builder()
                    .type(SocketPacket.Type.BINARY_EVENT)
//                    .attachmentsCount(1)
                    .data(list).build();
            ev.addAttachment(bytes);
            ctx.channel().writeAndFlush(ev);
        }
    }
}
