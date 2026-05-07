package com.ccl.engineio.netty.transport;

import com.ccl.engineio.core.protocol.EngineIOPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class WebSocketTransport extends SimpleChannelInboundHandler<WebSocketFrame>  {
    private final static Logger log = LoggerFactory.getLogger(WebSocketTransport.class);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        if (msg instanceof TextWebSocketFrame) {
            String content = ((TextWebSocketFrame) msg).text();
            if (log.isDebugEnabled()) {
                log.debug("WebSocketTextFrame => {}", content);
            }

            if (content.isEmpty()) {
                return;
            }

            char packetType = content.charAt(0);
            String data = content.length() > 1 ? content.substring(1) : null;

            switch (packetType) {
                case '2':
                    handlePing(ctx, data);
                    break;
                case '5':
                    handleUpgrade(ctx);
                    break;
                case '4':
                    handleMessage(ctx, data);
                    break;
                default:
                    if (log.isWarnEnabled()) {
                        log.warn("Unknown packet type: {}", packetType);
                    }
            }
        } else if (msg instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
            log.debug("BinaryWebSocketFrame => {}", frame.content().toString(StandardCharsets.UTF_8));
            ctx.fireChannelRead(frame.content());
        }
    }

    private void handlePing(ChannelHandlerContext ctx, String data) {
        if ("probe".equals(data)) {
            ctx.writeAndFlush(new TextWebSocketFrame("3probe"));
            if (log.isDebugEnabled()) {
                log.debug("Responded to probe ping");
            }
        } else {
            ctx.writeAndFlush(new TextWebSocketFrame("3"));
            if (log.isDebugEnabled()) {
                log.debug("Responded to ping");
            }
        }
    }

    private void handleUpgrade(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new TextWebSocketFrame("5"));
        if (log.isDebugEnabled()) {
            log.debug("Upgrade confirmed");
        }
    }

    private void handleMessage(ChannelHandlerContext ctx, String data) {
        if (log.isDebugEnabled()) {
            log.debug("Message received: {}", data);
        }
//        ctx.fireChannelRead(EngineIOPacket.of(EngineIOPacket.Type.MESSAGE, data));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete complete =
                    (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            if (log.isInfoEnabled()) {
                log.info("WebSocket handshake complete: {}", complete.requestUri());
            }
            ctx.fireUserEventTriggered(evt);
        } else if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
            if (log.isDebugEnabled()) {
                log.debug("Upgrade event: {}", evt);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
