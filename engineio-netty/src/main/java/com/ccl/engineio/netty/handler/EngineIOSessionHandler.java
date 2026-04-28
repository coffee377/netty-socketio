package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.core.session.SessionManager;
import io.netty.channel.*;
import io.netty.util.AttributeKey;

public class EngineIOSessionHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<ClientContext> CLIENT_CONTEXT = AttributeKey.valueOf("clientContext");
    private final SessionManager sessionManager;

    public EngineIOSessionHandler() {
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();

        if (sessionId != null && sessionManager.hasSession(sessionId)) {
            ClientContext context = sessionManager.getSession(sessionId);
            ctx.channel().attr(CLIENT_CONTEXT).set(context);
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
        ctx.close();
    }

    public static ClientContext getClientContext(ChannelHandlerContext ctx) {
        return ctx.channel().attr(CLIENT_CONTEXT).get();
    }
}
