package com.ccl.socketio.server.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 全局异常处理器
 *
 * <p>捕获 Channel 中未被上游处理器处理的异常，根据异常类型进行日志记录和连接管理。
 * IO 异常直接关闭连接，其他异常向客户端返回错误信息。
 *
 * @author coffee377
 * @since 4.0.0
 */
@Sharable
public class GlobalExceptionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (log.isErrorEnabled()) {
            log.error("Exception caught in channel: {}", ctx.channel(), cause);
        }

        if (ctx.channel().isActive()) {
            if (cause instanceof IOException) {
                ctx.close();
            } else {
                ctx.writeAndFlush(new TextWebSocketFrame("Error: " + cause.getMessage()))
                        .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }
}
