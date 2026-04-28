package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;

public class EngineIOUpgradeHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

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
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
