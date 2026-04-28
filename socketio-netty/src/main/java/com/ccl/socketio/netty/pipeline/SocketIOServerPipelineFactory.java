package com.ccl.socketio.netty.pipeline;

import com.ccl.engineio.netty.handler.*;
import com.ccl.socketio.core.event.EventRouter;
import com.ccl.socketio.core.namespace.NamespaceManager;
import com.ccl.socketio.netty.handler.SocketIOEventRouterHandler;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SocketIOServerPipelineFactory extends ChannelInitializer<Channel> {

    private final NamespaceManager namespaceManager;
    private final EventRouter eventRouter;
    private final long pingInterval;
    private final long pingTimeout;
    private final List<String> transports;
    private final Consumer<SocketIOEventRouterHandler> eventHandlerRegister;
    private final ChannelHandler businessHandler;
    private final ChannelHandler globalExceptionHandler;

    public SocketIOServerPipelineFactory(
            NamespaceManager namespaceManager,
            EventRouter eventRouter,
            long pingInterval,
            long pingTimeout,
            List<String> transports,
            Consumer<SocketIOEventRouterHandler> eventHandlerRegister,
            ChannelHandler businessHandler,
            ChannelHandler globalExceptionHandler) {
        this.namespaceManager = namespaceManager;
        this.eventRouter = eventRouter;
        this.pingInterval = pingInterval;
        this.pingTimeout = pingTimeout;
        this.transports = transports;
        this.eventHandlerRegister = eventHandlerRegister;
        this.businessHandler = businessHandler;
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // --- HTTP ---
        pipeline.addLast("httpCodec", new HttpServerCodec());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));

        // --- Idle state ---
//        pipeline.addLast("idleState", new IdleStateHandler(
//                (int) pingInterval / 1000,
//                (int) pingTimeout / 1000,
//                0, TimeUnit.SECONDS));

        // --- Handshake ---
        pipeline.addLast("engineHandshake", new EngineIOHandshakeHandler());

        // --- Engine.IO heartbeat ---
        pipeline.addLast("engineHeartbeat", new EngineIOHeartbeatHandler(pingInterval, pingTimeout));

        // --- WebSocket upgrade ---
        pipeline.addLast("wsUpgrade", new EngineIOUpgradeHandler());

        // --- Engine.IO codec: ByteBuf → EnginePacket ---
        pipeline.addLast("engineCodec", new EngineIOCodec());

//
//        // --- Engine.IO session management ---
//        pipeline.addLast("engineSession", new EngineIOSessionHandler());
//
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
