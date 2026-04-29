package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.DataType;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Engine.IO 编解码器
 *
 * <p>负责 ByteBuf 和 EngineIOPacket 之间的双向转换，处理 PING/PONG/CLOSE 等协议消息。
 */
public class EngineIOCodec extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(EngineIOCodec.class);

    private final ParserV4 parser;
    private final SessionManager sessionManager;

    public EngineIOCodec(int pingInterval, int pingTimeout) {
        this.parser = ParserV4.getInstance();
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * 入站：解码 ByteBuf -> EngineIOPacket，处理协议消息。
     *
     * @param ctx 通道上下文
     * @param msg 入站消息
     * @throws Exception 处理异常
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] bytes;
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            bytes = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), bytes);
            byteBuf.release();
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            bytes = new byte[frame.content().readableBytes()];
            frame.content().getBytes(frame.content().readerIndex(), bytes, 0, bytes.length);
        } else if (msg instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
            bytes = new byte[frame.content().readableBytes()];
            frame.content().getBytes(frame.content().readerIndex(), bytes);
        } else if (msg instanceof EngineIOPacket) {
            ctx.fireChannelRead(msg);
            return;
        } else {
            super.channelRead(ctx, msg);
            return;
        }

        EngineIOPacket<?> packet = parser.decodePacket(bytes, DataType.PLAINTEXT);
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (packet == null) {
            return;
        }

        switch (packet.getType()) {
            case PING:
                if (log.isDebugEnabled()) {
                    log.debug("Received PING from client: {}", ctx.channel().remoteAddress());
                }
                EngineIOPacket<Void> pongPacket = EngineIOPacket.of(EngineIOPacket.Type.PONG);
                ctx.channel().writeAndFlush(pongPacket);
                if (sessionId != null) {
                    sessionManager.updatePingTime(sessionId);
                }
                break;
            case PONG:
                if (log.isDebugEnabled()) {
                    log.debug("Received PONG from session: {}", sessionId);
                }
                break;
            case CLOSE:
                if (log.isDebugEnabled()) {
                    log.debug("Received CLOSE from client: {}", sessionId);
                }
                ctx.close();
                break;
            case MESSAGE:
            case UPGRADE:
            case NOOP:
            case OPEN:
                if (log.isDebugEnabled()) {
                    log.debug("EngineIO In: {}", packet.getType());
                }
                ctx.fireChannelRead(packet);
                break;
            default:
                if (log.isDebugEnabled()) {
                    log.debug("Passing through packet: {}", packet.getType());
                }
                ctx.fireChannelRead(packet);
        }
    }

    /**
     * 出站：编码 EngineIOPacket -> ByteBuf/WebSocketFrame。
     *
     * @param ctx    通道上下文
     * @param msg    出站消息
     * @param promise 通道操作承诺
     * @throws Exception 处理异常
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof EngineIOPacket<?>)) {
            super.write(ctx, msg, promise);
            return;
        }
        EngineIOPacket<?> packet = (EngineIOPacket<?>) msg;

        byte[] encoded = parser.encodePacket(packet, true);
        if (packet.getData() instanceof byte[]) {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(encoded);
            ctx.write(new BinaryWebSocketFrame(byteBuf), promise);
        } else {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(encoded);
            ctx.write(new TextWebSocketFrame(byteBuf), promise);
        }
    }
}
