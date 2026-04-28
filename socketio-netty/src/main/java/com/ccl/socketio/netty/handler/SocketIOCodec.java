package com.ccl.socketio.netty.handler;

import com.ccl.socketio.core.codec.SocketIODecoder;
import com.ccl.socketio.core.codec.SocketIOEncoder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class SocketIOCodec extends ChannelDuplexHandler {

    private final SocketIODecoder socketIODecoder = new SocketIODecoder();
    private final SocketIOEncoder socketIOEncoder = new SocketIOEncoder();

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        super.read(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

}
