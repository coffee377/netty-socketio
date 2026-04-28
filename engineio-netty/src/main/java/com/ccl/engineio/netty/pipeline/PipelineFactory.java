package com.ccl.engineio.netty.pipeline;

import com.ccl.engineio.netty.handler.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * Pipeline 工厂
 * 负责初始化 Netty Channel Pipeline，按照协议栈顺序添加各层处理器：
 * HTTP Codec -> Engine.IO 握手 -> WebSocket 升级 -> 编解码 -> 心跳 -> Session 管理
 */
public class PipelineFactory extends ChannelInitializer<Channel> {

    private final String connectPath;
    private final long pingInterval;
    private final long pingTimeout;
    private final int maxFramePayloadLength;

    public PipelineFactory(String connectPath, long pingInterval, long pingTimeout, int maxFramePayloadLength) {
        this.connectPath = connectPath;
        this.pingInterval = pingInterval;
        this.pingTimeout = pingTimeout;
        this.maxFramePayloadLength = maxFramePayloadLength;
    }

    /**
     * 初始化 Channel Pipeline
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // --- HTTP ---
        pipeline.addLast("httpCodec", new HttpServerCodec());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(maxFramePayloadLength));

        // --- Engine.IO 握手 + Session 创建 ---
        pipeline.addLast("engineHandshake", new EngineIOHandshakeHandler(connectPath, maxFramePayloadLength));

        // --- Engine.IO 编解码 ---
        pipeline.addLast("engineCodec", new EngineIOCodec((int) pingInterval, (int) pingTimeout));

        // --- WebSocket 升级 ---
        pipeline.addLast("wsUpgrade", new EngineIOUpgradeHandler());

        // --- Engine.IO WebSocket 协议处理 ---
        pipeline.addLast("wsProtocol", new WebSocketServerProtocolHandler(null, true, maxFramePayloadLength));

        // --- Engine.IO 心跳 ---
        pipeline.addLast("engineHeartbeat", new EngineIOHeartbeatHandler(pingInterval, pingTimeout));

        // --- Engine.IO Session 生命周期 ---
        pipeline.addLast("engineSession", new EngineIOSessionHandler());
    }
}
