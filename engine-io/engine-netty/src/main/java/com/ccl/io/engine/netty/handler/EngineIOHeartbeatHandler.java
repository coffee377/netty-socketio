package com.ccl.io.engine.netty.handler;

import com.ccl.io.engine.core.session.SessionManager;
import com.ccl.io.engine.protocol.EngineIOPacket;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * Engine.IO 心跳处理器
 *
 * <p>继承自 Netty 的 {@link IdleStateHandler}，负责检测连接空闲状态并发送心跳：
 * <ul>
 *   <li>READER_IDLE：读空闲超时，移除会话并关闭连接</li>
 *   <li>WRITER_IDLE：写空闲超时，发送 PING 消息维持连接</li>
 * </ul>
 *
 * @see <a href="https://socket.io/docs/v4/engine-io-protocol/#heartbeat">Engine.IO Heartbeat</a>
 */
public class EngineIOHeartbeatHandler extends IdleStateHandler {

    private final SessionManager sessionManager;

    public EngineIOHeartbeatHandler(long pingInterval, long pingTimeout) {
        super(pingInterval, pingTimeout, 0, TimeUnit.MILLISECONDS);
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * 处理连接空闲事件
     *
     * <p>读空闲时移除 Session 并关闭连接；写空闲时发送 PING 心跳包
     *
     * @param ctx Channel 处理器上下文
     * @param evt 空闲状态事件
     */
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
        EngineIOPacket<?> pingPacket = EngineIOPacket.builder().type(EngineIOPacket.Type.PING).build();
        ctx.writeAndFlush(pingPacket);
    }

    /**
     * 触发用户事件，处理空闲状态事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            channelIdle(ctx, idleStateEvent);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
