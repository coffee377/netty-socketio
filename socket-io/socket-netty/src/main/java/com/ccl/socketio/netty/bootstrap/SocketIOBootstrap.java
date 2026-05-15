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

/**
 * Socket.IO Netty 服务端启动引导类
 *
 * <p>封装 Netty ServerBootstrap 的配置和启动流程，提供端口、传输层、事件处理等配置能力。
 * 支持 WebSocket 和 Polling 两种传输模式。
 *
 * @author coffee377
 * @since 4.0.0
 */
public class SocketIOBootstrap {

    private int port = 3000;
    private List<String> transports = Arrays.asList("websocket", "polling");
    private Consumer<SocketIOEventRouterHandler> eventHandlerRegister;
    private ChannelHandler businessHandler;
    private ChannelHandler globalExceptionHandler;
    private boolean enableCors = true;
    private String corsOrigin = "*";

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel serverChannel;
    private NamespaceManager namespaceManager;
    private EventRouter eventRouter;


    /**
     * 创建默认引导启动器，监听端口 3000
     */
    public SocketIOBootstrap() {
    }

    /**
     * 创建引导启动器并指定监听端口
     *
     * @param port 监听端口号
     */
    public SocketIOBootstrap(int port) {
        this.port = port;
    }

    /**
     * 创建引导启动器并指定详细配置
     *
     * @param port         监听端口号
     * @param pingInterval Ping 心跳间隔（毫秒）
     * @param pingTimeout  Ping 心跳超时（毫秒）
     * @param transports   启用的传输层列表
     */
    public SocketIOBootstrap(int port, long pingInterval, long pingTimeout, List<String> transports) {
        this.port = port;
        this.transports = transports;
    }

    /**
     * 设置事件处理器注册回调
     *
     * @param eventHandlerRegister 事件处理器注册回调
     * @return 当前引导启动器实例
     */
    public SocketIOBootstrap setEventHandlerRegister(Consumer<SocketIOEventRouterHandler> eventHandlerRegister) {
        this.eventHandlerRegister = eventHandlerRegister;
        return this;
    }

    /**
     * 设置业务逻辑处理器
     *
     * @param businessHandler 业务 ChannelHandler
     * @return 当前引导启动器实例
     */
    public SocketIOBootstrap setBusinessHandler(ChannelHandler businessHandler) {
        this.businessHandler = businessHandler;
        return this;
    }

    /**
     * 设置全局异常处理器
     *
     * @param globalExceptionHandler 异常 ChannelHandler
     * @return 当前引导启动器实例
     */
    public SocketIOBootstrap setGlobalExceptionHandler(ChannelHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
        return this;
    }

    /**
     * 设置是否启用 CORS
     *
     * @param enableCors 是否启用 CORS
     * @return 当前引导启动器实例
     */
    public SocketIOBootstrap setEnableCors(boolean enableCors) {
        this.enableCors = enableCors;
        return this;
    }

    /**
     * 设置 CORS 允许的域名
     *
     * @param corsOrigin 允许的域名，默认为 "*"
     * @return 当前引导启动器实例
     */
    public SocketIOBootstrap setCorsOrigin(String corsOrigin) {
        this.corsOrigin = corsOrigin;
        return this;
    }

    /**
     * 启动 Socket.IO 服务端
     *
     * @throws InterruptedException 启动过程被中断时抛出
     */
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
                        globalExceptionHandler,
                        enableCors,
                        corsOrigin))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
    }

    /**
     * 停止 Socket.IO 服务端，释放所有资源
     */
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
