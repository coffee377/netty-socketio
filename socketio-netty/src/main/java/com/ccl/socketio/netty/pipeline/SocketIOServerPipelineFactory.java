package com.ccl.socketio.netty.pipeline;

import com.ccl.engineio.netty.handler.*;
import com.ccl.engineio.netty.transport.PollingTransport;
import com.ccl.socketio.core.event.EventRouter;
import com.ccl.socketio.core.namespace.NamespaceManager;
import com.ccl.socketio.netty.handler.SocketIOEventRouterHandler;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Socket.IO 服务端 ChannelPipeline 工厂
 *
 * <p>负责构建 Netty Channel 的处理流水线，组织各类 Handler 的执行顺序。
 * 流水线从上到下分为以下几个阶段：
 * <ul>
 *   <li>HTTP 编解码：HttpServerCodec、HttpObjectAggregator</li>
 *   <li>Engine.IO 握手：EngineIOHandshakeHandler 处理连接建立</li>
 *   <li>WebSocket 升级：EngineIOUpgradeHandler 处理协议升级</li>
 *   <li>Engine.IO 编解码：EngineIOCodec 处理数据帧与协议包的转换</li>
 * </ul>
 */
public class SocketIOServerPipelineFactory extends ChannelInitializer<Channel> {

    private final NamespaceManager namespaceManager;
    private final EventRouter eventRouter;
    private final long pingInterval;
    private final long pingTimeout;
    private final List<String> transports;
    private final Consumer<SocketIOEventRouterHandler> eventHandlerRegister;
    private final ChannelHandler businessHandler;
    private final ChannelHandler globalExceptionHandler;
    private final boolean enableCors;
    private final String corsOrigin;

    public SocketIOServerPipelineFactory(
            NamespaceManager namespaceManager,
            EventRouter eventRouter,
            long pingInterval,
            long pingTimeout,
            List<String> transports,
            Consumer<SocketIOEventRouterHandler> eventHandlerRegister,
            ChannelHandler businessHandler,
            ChannelHandler globalExceptionHandler) {
        this(namespaceManager, eventRouter, pingInterval, pingTimeout, transports,
                eventHandlerRegister, businessHandler, globalExceptionHandler, true, "*");
    }

    public SocketIOServerPipelineFactory(
            NamespaceManager namespaceManager,
            EventRouter eventRouter,
            long pingInterval,
            long pingTimeout,
            List<String> transports,
            Consumer<SocketIOEventRouterHandler> eventHandlerRegister,
            ChannelHandler businessHandler,
            ChannelHandler globalExceptionHandler,
            boolean enableCors,
            String corsOrigin) {
        this.namespaceManager = namespaceManager;
        this.eventRouter = eventRouter;
        this.pingInterval = pingInterval;
        this.pingTimeout = pingTimeout;
        this.transports = transports;
        this.eventHandlerRegister = eventHandlerRegister;
        this.businessHandler = businessHandler;
        this.globalExceptionHandler = globalExceptionHandler;
        this.enableCors = enableCors;
        this.corsOrigin = corsOrigin;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 1. HTTP 基础编解码器
        HttpServerCodec httpCodec = new HttpServerCodec();
        pipeline.addLast("httpCodec", httpCodec);

        // 2. 协议升级处理器
        pipeline.addLast("upgrade", new EngineIOUpgradeHandler(httpCodec));

        // 3. 聚合完整HTTP请求
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));
//        pipeline.addLast("chunked", new ChunkedWriteHandler());

        // --- Idle state ---
//        pipeline.addLast("idleState", new IdleStateHandler(
//                (int) pingInterval / 1000,
//                (int) pingTimeout / 1000,
//                0, TimeUnit.SECONDS));

        // EngineIO握手处理器（核心）
        pipeline.addLast("engineHandshake", new EngineIOHandshakeHandler("/socket.io", 65536, enableCors, corsOrigin));

        // --- Engine.IO heartbeat ---
        // pipeline.addLast("engineHeartbeat", new EngineIOHeartbeatHandler(pingInterval, pingTimeout));

        // Engine.IO Polling 处理器 4. 长轮询业务处理器（处理 polling 握手、GET/POST）
        pipeline.addLast("polling", new PollingTransport());

        // --- Engine.IO codec: ByteBuf → EnginePacket ---
        pipeline.addLast("engineCodec", new EngineIOCodec((int) pingInterval, (int) pingTimeout));

        // --- Engine.IO session management ---
//        pipeline.addLast("engineSession", new EngineIOSessionHandler());

//        // --- Socket.IO codec: EnginePacket → SocketPacket ---
//        pipeline.addLast("socketIOCodec", new SocketIOCodecHandler());
//
//        // --- Socket.IO namespace management ---
//        pipeline.addLast("socketIONamespace", new SocketIONamespaceHandler(namespaceManager));
//
//        // --- Socket.IO binary attachment ---
//        pipeline.addLast("socketIOBinary", new SocketIOBinaryHandler());
//
//        // --- Socket.IO event router ---
//        SocketIOEventRouterHandler routerHandler = new SocketIOEventRouterHandler(eventRouter);
//        pipeline.addLast("socketIORouter", routerHandler);
//
//        // --- Business handler ---
//        if (businessHandler != null) {
//            pipeline.addLast("businessHandler", businessHandler);
//        }
//
//        // --- Global exception handler ---
//        if (globalExceptionHandler != null) {
//            pipeline.addLast("exceptionHandler", globalExceptionHandler);
//        }
//
//        // Register event handler callback
//        if (eventHandlerRegister != null) {
//            try {
//                eventHandlerRegister.accept(routerHandler);
//            } catch (Exception e) {
//                // Ignore callback errors
//            }
//        }
    }
}
