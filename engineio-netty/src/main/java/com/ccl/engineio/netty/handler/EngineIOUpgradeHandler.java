package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.netty.WebSocketUtil;
import com.ccl.engineio.netty.transport.WebSocketTransport;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class EngineIOUpgradeHandler extends HttpServerUpgradeHandler {

    private final static Logger log = LoggerFactory.getLogger(EngineIOUpgradeHandler.class);

    public EngineIOUpgradeHandler(SourceCodec sourceCodec) {
        super(new SourceCodec() {
            @Override
            public void upgradeFrom(ChannelHandlerContext ctx) {

            }
        }, new EngineIOUpgradeCodecFactory());
    }

    static class EngineIOUpgradeCodecFactory implements HttpServerUpgradeHandler.UpgradeCodecFactory {
        @Override
        public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            // 仅支持 WebSocket 升级
            if (TransportType.WEBSOCKET.getName().contentEquals(protocol)) {
                return new EngineIOWebSocketUpgradeCodec();
            }
            return null;
        }
    }

    static class EngineIOWebSocketUpgradeCodec implements HttpServerUpgradeHandler.UpgradeCodec {
        @Override
        public Collection<CharSequence> requiredUpgradeHeaders() {
            return Collections.singletonList(HttpHeaderNames.UPGRADE);
        }

        @Override
        public boolean prepareUpgradeResponse(ChannelHandlerContext ctx, FullHttpRequest upgradeRequest, HttpHeaders upgradeHeaders) {
            // 添加WebSocket握手所需头信息
            // upgradeHeaders.set(HttpHeaderNames.UPGRADE, "websocket");
            // upgradeHeaders.set(HttpHeaderNames.CONNECTION, "Upgrade");
            String key = upgradeRequest.headers().get(HttpHeaderNames.SEC_WEBSOCKET_KEY);
            String accept = WebSocketUtil.calculateAccept(key);
            upgradeHeaders.set(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT, accept);

            return true;
        }

        @Override
        public void upgradeTo(ChannelHandlerContext ctx, FullHttpRequest upgradeRequest) {
            // 升级完成，替换Pipeline处理器
            ChannelPipeline pipeline = ctx.pipeline();

            // 移除HTTP相关处理器
//            pipeline.remove(HttpServerCodec.class);
//            pipeline.remove(HttpObjectAggregator.class);
//            pipeline.remove(ChunkedWriteHandler.class);
//            pipeline.remove(EngineIOUpgradeHandler.class);
//            pipeline.remove(PollingHandler.class);

            // 添加WebSocket处理器
            WebSocketServerProtocolConfig config = WebSocketServerProtocolConfig.newBuilder()
                    .websocketPath("/socket.io/")
                    .checkStartsWith(true)
                    .handleCloseFrames(true)
                    .build();
            WebSocketServerProtocolHandler webSocketServerProtocolHandler = new WebSocketServerProtocolHandler(config);

            pipeline.addLast(webSocketServerProtocolHandler);
            pipeline.addLast(new WebSocketTransport());

            // 标记会话为WebSocket模式
            String sid = ctx.channel().attr(ChannelAttributes.SESSION_ID).get();
//            session.setTransport("websocket");
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }
}
