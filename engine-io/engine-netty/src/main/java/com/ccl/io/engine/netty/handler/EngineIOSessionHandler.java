package com.ccl.io.engine.netty.handler;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.core.store.MemoryEngineClientStore;
import com.ccl.io.engine.store.EngineClientStore;
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
 * @since 4.0.0
 */
public class EngineIOSessionHandler extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<EngineClient> ENGINE_CLIENT = AttributeKey.valueOf("engine_client");

    private final EngineClientStore<?> sessionStore;

    public EngineIOSessionHandler(EngineClientStore<?> sessionStore) {
        this.sessionStore = sessionStore;
    }

    public EngineIOSessionHandler() {
        this(new MemoryEngineClientStore());
    }

    /**
     * 从 SessionManager 获取当前 Channel 的 Session 上下文并缓存到 Channel 属性中
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (ctx.channel().attr(ChannelAttributes.ENGINE_CLIENT).get() == null) {
            String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
            if (sessionId != null && sessionStore.hasClient(sessionId)) {
                EngineClient client = sessionStore.getClient(sessionId);
                ctx.channel().attr(ChannelAttributes.ENGINE_CLIENT).set(client);
            }
        }
        super.channelRead(ctx, msg);
    }

    /**
     * 连接断开时移除对应的 Session
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        removeClient(ctx);
        super.channelInactive(ctx);
    }

    /**
     * 处理异常时移除 Session 并关闭连接
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        removeClient(ctx);
        ctx.close();
    }

    private void removeClient(ChannelHandlerContext ctx) {
        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null &&  sessionStore.hasClient(sessionId)) {
            sessionStore.removeClient(sessionId);
        }
    }

}
