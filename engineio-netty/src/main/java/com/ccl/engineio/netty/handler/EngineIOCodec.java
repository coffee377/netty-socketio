package com.ccl.engineio.netty.handler;

import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineIOCodec extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(EngineIOCodec.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.trace("EngineIO In: {}", "");
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log.trace("EngineIO Out: {}", "");
        super.write(ctx, msg, promise);
    }

}
