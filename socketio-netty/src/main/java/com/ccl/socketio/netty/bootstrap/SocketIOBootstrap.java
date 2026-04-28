package com.ccl.socketio.netty.bootstrap;

import com.ccl.socketio.core.event.EventRouter;
import com.ccl.socketio.core.namespace.NamespaceManager;
import com.ccl.socketio.netty.handler.SocketIOEventRouterHandler;
import com.ccl.socketio.netty.pipeline.SocketIOServerPipelineFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SocketIOBootstrap {

    private int port = 3000;
    private List<String> transports = Arrays.asList("websocket", "polling");
    private Consumer<SocketIOEventRouterHandler> eventHandlerRegister;
    private ChannelHandler businessHandler;
    private ChannelHandler globalExceptionHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel serverChannel;
    private NamespaceManager namespaceManager;
    private EventRouter eventRouter;


    public SocketIOBootstrap() {
    }

    public SocketIOBootstrap(int port) {
        this.port = port;
    }

    public SocketIOBootstrap(int port, long pingInterval, long pingTimeout, List<String> transports) {
        this.port = port;
        this.transports = transports;
    }

    public SocketIOBootstrap setEventHandlerRegister(Consumer<SocketIOEventRouterHandler> eventHandlerRegister) {
        this.eventHandlerRegister = eventHandlerRegister;
        return this;
    }

    public SocketIOBootstrap setBusinessHandler(ChannelHandler businessHandler) {
        this.businessHandler = businessHandler;
        return this;
    }

    public SocketIOBootstrap setGlobalExceptionHandler(ChannelHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
        return this;
    }

    public void start() throws InterruptedException {
        IoHandlerFactory handler = NioIoHandler.newFactory();
        bossGroup = new MultiThreadIoEventLoopGroup(0, handler);
        workerGroup = new MultiThreadIoEventLoopGroup(0, handler);

        namespaceManager = NamespaceManager.getInstance();
        eventRouter = EventRouter.getInstance();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new SocketIOServerPipelineFactory(
                        namespaceManager,
                        eventRouter,
                        30000,
                        25000,
                        transports,
                        eventHandlerRegister,
                        businessHandler,
                        globalExceptionHandler))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

}
