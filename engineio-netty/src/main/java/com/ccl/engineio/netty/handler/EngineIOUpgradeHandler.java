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
public class EngineIOUpgradeHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(EngineIOUpgradeHandler.class);

    private final SessionManager sessionManager;

    public EngineIOUpgradeHandler() {
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            ByteBuf buffer = textFrame.content();
            ctx.fireChannelRead(buffer.retainedSlice());
        } else if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
            ctx.fireChannelRead(binaryFrame.content().retainedSlice());
        }
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
