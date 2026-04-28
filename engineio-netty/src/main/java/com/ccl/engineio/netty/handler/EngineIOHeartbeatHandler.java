package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.session.SessionManager;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class EngineIOHeartbeatHandler extends IdleStateHandler {

    private final SessionManager sessionManager;

    public EngineIOHeartbeatHandler(long pingInterval, long pingTimeout) {
        super(pingInterval, pingTimeout, 0, TimeUnit.MILLISECONDS);
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (evt.state() == IdleState.READER_IDLE) {
            String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
            if (sessionId != null) {
                sessionManager.removeSession(sessionId);
            }
            ctx.close();
        } else if (evt.state() == IdleState.WRITER_IDLE) {
            sendPing(ctx);
        }
    }

    private void sendPing(ChannelHandlerContext ctx) {
        EngineIOPacket<?> pingPacket = EngineIOPacket.of(EngineIOPacket.Type.PING);
        ctx.writeAndFlush(pingPacket);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            channelIdle(ctx, idleStateEvent);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
