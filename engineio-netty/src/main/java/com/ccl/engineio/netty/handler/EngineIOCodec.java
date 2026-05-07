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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.CharsetUtil;
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
        if (!(msg instanceof EngineIOPacket)) {
            super.channelRead(ctx, msg);
            return;
        }

        log.info("EngineIOCodec read");

        @SuppressWarnings("unchecked")
        EngineIOPacket<ByteBuf> packet = (EngineIOPacket<ByteBuf>) msg;
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();

        switch (packet.getType()) {
            case PING:
                if (log.isDebugEnabled()) {
                    log.debug("Received PING from client: {}", ctx.channel().remoteAddress());
                }
                EngineIOPacket<?> pongPacket = EngineIOPacket.builder().type(EngineIOPacket.Type.PONG).build();
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
                if (log.isDebugEnabled()) {
                    log.debug("Received MESSAGE {} from session: {}", packet.getData().toString(CharsetUtil.UTF_8), sessionId);
                }
                EngineIOPacket<String> p = EngineIOPacket.builder()
                        .data(String.format("0{\"sid\":\"%s\"}", sessionId))
                        .build();
                ctx.channel().writeAndFlush(p);
                break;
            case UPGRADE:
            case NOOP:
            case OPEN:
                if (log.isDebugEnabled()) {
                    log.debug("EngineIO In: {}", packet.getType());
                }
//                ctx.writeAndFlush(packet);
//                ctx.fireChannelRead(packet);
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
     * @param ctx     通道上下文
     * @param msg     出站消息
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
//        ByteBuf byteBuf = Unpooled.wrappedBuffer(encoded);
        byte[] encoded = parser.encodePacket(packet, true);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.content().writeBytes(encoded);
        ctx.writeAndFlush(response);


//        if (packet.getData() instanceof byte[]) {
//            ByteBuf byteBuf = Unpooled.wrappedBuffer(encoded);
//            ctx.write(new BinaryWebSocketFrame(byteBuf), promise);
//        } else {
//            ByteBuf byteBuf = Unpooled.wrappedBuffer(encoded);
//            ctx.write(new TextWebSocketFrame(byteBuf), promise);
//        }
    }
}
