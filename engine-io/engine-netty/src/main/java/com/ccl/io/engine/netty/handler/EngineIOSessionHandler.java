package com.ccl.io.engine.netty.handler;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.store.EngineClientStore;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.Collections;

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

    private final EngineClientStore<?> sessionStore;

    public EngineIOSessionHandler(EngineClientStore<?> sessionStore) {
        this.sessionStore = sessionStore;
    }

    /**
     * 从 SessionManager 获取当前 Channel 的 Session 上下文并缓存到 Channel 属性中
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (ctx.channel().attr(ChannelAttributes.ENGINE_CLIENT).get() == null) {
            String sessionId = getSessionIdFrom(msg);
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
        removeClient(ctx.channel());
        super.channelInactive(ctx);
    }

    private String getSessionIdFrom(Object msg) {
        if (msg instanceof FullHttpRequest) {
            return getSessionIdFromRequest((FullHttpRequest) msg);
        }
        return null;
    }

    private String getSessionIdFromRequest(FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        return decoder.parameters().getOrDefault("sid", Collections.emptyList()).stream().findFirst().orElse(null);
    }


    private void removeClient(Channel channel) {
        EngineClient client = channel.attr(ChannelAttributes.ENGINE_CLIENT).get();
        if (client != null && sessionStore.hasClient(client.getSessionId())) {
            sessionStore.removeClient(client.getSessionId());
        }
    }

}
