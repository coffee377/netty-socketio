package com.ccl.engineio.netty.pipeline;

import com.ccl.engineio.netty.handler.*;
import com.ccl.engineio.netty.transport.PollingTransport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Pipeline 工厂
 * 负责初始化 Netty Channel Pipeline，按照协议栈顺序添加各层处理器：
 * HTTP Codec -> Engine.IO 握手 -> WebSocket 升级/Polling -> 编解码 -> 心跳 -> Session 管理
 * <p>
 * WebSocketServerProtocolHandler 会在握手成功且 transport=websocket 时动态添加，不在初始 pipeline 中。
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
    }
}
