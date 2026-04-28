package com.ccl.engineio.netty.transport;

import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.netty.handler.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 传输路由处理器
 * 负责根据请求信息判断传输类型（Polling 或 WebSocket），并执行路由逻辑
 */
public class TransportRouteHandler {

    private static final Logger log = LoggerFactory.getLogger(TransportRouteHandler.class);

    /**
     * 判断传输类型
     */
    public TransportType determineTransportType(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        HttpRequest req = channel.attr(ChannelAttributes.HTTP_REQUEST).get();
        if (req != null) {
            String connection = req.headers().get(io.netty.handler.codec.http.HttpHeaderNames.CONNECTION, "");
            if (connection.toLowerCase().contains("upgrade")) {
                return TransportType.WEBSOCKET;
            }
        }
        return TransportType.POLLING;
    }

    /**
     * 执行路由逻辑
     */
    public void route(ChannelHandlerContext ctx, TransportType transportType) {
        if (log.isDebugEnabled()) {
            log.debug("Routing channel to transport: {} for {}", transportType.name(), ctx.channel().remoteAddress());
        }

        String sessionId = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
        if (sessionId != null) {
            com.ccl.engineio.core.entity.ClientContext context = com.ccl.engineio.core.session.SessionManager.getInstance()
                    .getSession(sessionId);
            context.setTransportType(transportType);
        }
    }
}
