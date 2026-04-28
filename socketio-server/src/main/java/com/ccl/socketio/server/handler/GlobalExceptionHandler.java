package com.ccl.socketio.server.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class GlobalExceptionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught in channel: {}", ctx.channel(), cause);

        if (ctx.channel().isActive()) {
            if (cause instanceof java.io.IOException) {
                ctx.close();
            } else {
                ctx.writeAndFlush(new TextWebSocketFrame("Error: " + cause.getMessage()))
                        .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Channel inactive: {}", ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        logger.debug("User event triggered: {} on channel: {}", evt, ctx.channel());
        super.userEventTriggered(ctx, evt);
    }
}
