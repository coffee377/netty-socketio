package com.ccl.io.engine.netty.handler;

import com.ccl.io.engine.netty.WebSocketUtil;
import com.ccl.io.engine.netty.transport.WebSocketTransport;
import com.ccl.io.engine.protocol.Transport;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Engine.IO WebSocket 升级处理器
 *
 * <p>处理 HTTP 升级到 WebSocket 协议的过程：
 * <ul>
 *   <li>拦截 WebSocket 升级请求</li>
 *   <li>计算 Sec-WebSocket-Accept 响应头</li>
 *   <li>升级完成后替换 Pipeline 为 WebSocket 处理器</li>
 * </ul>
 *
 * @author coffee377
 * @since 4.0.0
 */
public class EngineIOUpgradeHandler extends HttpServerUpgradeHandler {

    private final static Logger log = LoggerFactory.getLogger(EngineIOUpgradeHandler.class);

    public EngineIOUpgradeHandler(SourceCodec sourceCodec) {
        super(new SourceCodec() {
            @Override
            public void upgradeFrom(ChannelHandlerContext ctx) {

            }
        }, new EngineIOUpgradeCodecFactory());
    }

    /**
     * 升级编解码器工厂
     *
     * <p>根据请求的协议名称创建对应的升级编解码器，当前仅支持 WebSocket 协议升级
     */
    static class EngineIOUpgradeCodecFactory implements HttpServerUpgradeHandler.UpgradeCodecFactory {
        @Override
        public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            // 仅支持 WebSocket 升级
            if (Transport.WEBSOCKET.getName().contentEquals(protocol)) {
                return new EngineIOWebSocketUpgradeCodec();
            }
            return null;
        }
    }

    /**
     * Engine.IO WebSocket 升级编解码器
     *
     * <p>实现 HttpServerUpgradeHandler.UpgradeCodec 接口，处理 WebSocket 协议升级的握手细节：
     * <ul>
     *   <li>声明所需的升级请求头</li>
     *   <li>计算并设置 Sec-WebSocket-Accept 响应头</li>
     *   <li>升级完成后替换 Pipeline 中的处理器</li>
     * </ul>
     */
    static class EngineIOWebSocketUpgradeCodec implements HttpServerUpgradeHandler.UpgradeCodec {
        /**
         * 返回所需的升级请求头集合
         *
         * @return 包含 UPGRADE 头的集合
         */
        @Override
        public Collection<CharSequence> requiredUpgradeHeaders() {
            return Collections.singletonList(HttpHeaderNames.UPGRADE);
        }

        /**
         * 准备升级响应，计算 WebSocket Accept 头
         *
         * @param ctx            Channel 处理器上下文
         * @param upgradeRequest 升级请求
         * @param upgradeHeaders 升级响应头
         * @return 是否准备成功
         */
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

        /**
         * 执行升级，将 Pipeline 替换为 WebSocket 处理器
         *
         * @param ctx            Channel 处理器上下文
         * @param upgradeRequest 升级请求
         */
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
