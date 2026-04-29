package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Engine.IO WebSocket 升级处理器
 *
 * <p>处理 WebSocket 握手完成事件，标记传输升级为 WebSocket 模式。
 */
public class EngineIOUpgradeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(EngineIOUpgradeHandler.class);

    private final SessionManager sessionManager;

    public EngineIOUpgradeHandler() {
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("EngineIOUpgradeHandler");
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) msg;
            ByteBuf buffer = textFrame.content();
            ctx.fireChannelRead(buffer.retainedSlice());
        } else if (msg instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) msg;
            ctx.fireChannelRead(binaryFrame.content().retainedSlice());
        }
        super.channelRead(ctx, msg);
    }

    public void chanelRead(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete =
                    (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String uri = handshakeComplete.requestUri();
            if (uri.contains("transport=websocket")) {
                handleWebSocketUpgrade(ctx);
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    private void handleWebSocketUpgrade(ChannelHandlerContext ctx) {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId == null) {
            log.warn("WebSocket upgrade attempted without session");
            ctx.close();
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("WebSocket upgrade for session: {}", sessionId);
        }

        com.ccl.engineio.core.entity.ClientContext clientContext = sessionManager.getSession(sessionId);
        clientContext.upgradeTransport();

        ctx.writeAndFlush(EngineIOPacket.of(EngineIOPacket.Type.UPGRADE));

        if (log.isDebugEnabled()) {
            log.debug("WebSocket upgrade completed for session: {}", sessionId);
        }
    }
}
