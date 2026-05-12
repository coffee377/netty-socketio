package com.ccl.engineio.netty.handler;

import com.ccl.io.engine.core.entity.ClientContext;
import com.ccl.io.engine.core.session.SessionManager;
import io.netty.channel.*;
import io.netty.util.AttributeKey;

/**
 * Engine.IO 会话生命周期处理器
 *
 * <p>在请求处理的入站路径上拦截消息，将 Session 上下文绑定到 Channel 的 AttributeMap：
 * <ul>
 *   <li>从 SessionManager 获取当前 Channel 的 Session 上下文并缓存</li>
 *   <li>在连接断开时自动清理 Session</li>
 *   <li>在异常发生时移除 Session 并关闭连接</li>
 * </ul>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class EngineIOSessionHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<ClientContext> CLIENT_CONTEXT = AttributeKey.valueOf("clientContext");
    private final SessionManager sessionManager;

    public EngineIOSessionHandler() {
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * 从 SessionManager 获取当前 Channel 的 Session 上下文并缓存到 Channel 属性中
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();

        if (sessionId != null && sessionManager.hasSession(sessionId)) {
            ClientContext context = sessionManager.getSession(sessionId);
            ctx.channel().attr(CLIENT_CONTEXT).set(context);
        }

        super.channelRead(ctx, msg);
    }

    /**
     * 连接断开时移除对应的 Session
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
        super.channelInactive(ctx);
    }

    /**
     * 处理异常时移除 Session 并关闭连接
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
        ctx.close();
    }

    /**
     * 获取当前 Channel 绑定的客户端上下文
     *
     * @param ctx Channel 处理器上下文
     * @return 客户端上下文，未绑定时返回 null
     */
    public static ClientContext getClientContext(ChannelHandlerContext ctx) {
        return ctx.channel().attr(CLIENT_CONTEXT).get();
    }
}
