package com.ccl.engineio.netty.transport;

import com.ccl.engineio.core.protocol.TransportType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;

public class TransportRouteHandler {

    public TransportType determineTransportType(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        HttpRequest req = channel.attr(WEBSOCKET_REQ).get();
        if (req != null) {
            String connection = req.headers().get("Connection", "");
            if (connection.toLowerCase().contains("upgrade")) {
                return TransportType.WEBSOCKET;
            }
        }
        return TransportType.POLLING;
    }

    public void route(ChannelHandlerContext ctx, TransportType transportType) {
    }

    public static final AttributeKey<HttpRequest> WEBSOCKET_REQ = AttributeKey.valueOf("websocketRequest");
}
