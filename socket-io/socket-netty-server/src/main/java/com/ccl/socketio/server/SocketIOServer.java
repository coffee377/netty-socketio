package com.ccl.socketio.server;

import com.ccl.socketio.core.namespace.impl.Namespace;
import com.ccl.socketio.netty.bootstrap.SocketIOBootstrap;
import com.ccl.socketio.server.config.ServerConfig;
import com.ccl.socketio.server.config.ServerOptions;
import com.ccl.socketio.server.handler.BusinessEventHandler;
import com.ccl.socketio.server.handler.GlobalExceptionHandler;
import com.ccl.socketio.server.listener.SocketIOListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socket.IO 服务端主入口
 *
 * <p>提供服务端的配置、启动、停止和事件注册功能。
 * 使用 {@link SocketIOBootstrap} 启动 Netty 服务，通过 {@link SocketIOListener} 处理业务逻辑。
 *
 * @author coffee377
 * @since 4.0.0
 */
public class SocketIOServer {

    private static final Logger log = LoggerFactory.getLogger(SocketIOServer.class);

    private final SocketIOBootstrap bootstrap;
    private final ServerConfig config;
    private SocketIOListener listener;

    private SocketIOServer(ServerConfig config) {
        this.config = config;
        ServerOptions options = config.getOptions();
        this.bootstrap = new SocketIOBootstrap(options.getPort())
                .setEnableCors(options.isAllowCors())
                .setCorsOrigin(options.getCorsOrigin());
    }

    /**
     * 启动 Socket.IO 服务端
     */
    public void start() {
        try {
            bootstrap.start();
            log.info("Socket.IO server started on port {}", config.getOptions().getPort());
        } catch (InterruptedException e) {
            log.error("Failed to start server", e);
            Thread.currentThread().interrupt();
        }
    }


    /**
     * 停止 Socket.IO 服务端
     */
    public void stop() {
        bootstrap.stop();
        log.info("Socket.IO server stopped");
    }

    /**
     * 设置事件监听器
     *
     * @param listener SocketIO 事件监听器
     * @return 当前服务端实例
     */
    public SocketIOServer setListener(SocketIOListener listener) {
        this.listener = listener;
        bootstrap.setBusinessHandler(new BusinessEventHandler(listener));
        bootstrap.setGlobalExceptionHandler(new GlobalExceptionHandler());
        return this;
    }

    /**
     * 注册命名空间下的事件处理器
     *
     * @param namespace 命名空间名称
     * @param eventName 事件名称
     * @param handler   事件处理回调
     */
    public void onEvent(String namespace, String eventName, java.util.function.Consumer<Namespace.SocketClient> handler) {
        bootstrap.setEventHandlerRegister(router -> {
            router.registerEventHandler(namespace, eventName, handler);
        });
    }

    /**
     * 获取事件监听器
     *
     * @return SocketIO 事件监听器
     */
    public SocketIOListener getListener() {
        return listener;
    }

    /**
     * 获取 Netty 引导启动器
     *
     * @return SocketIOBootstrap 实例
     */
    public SocketIOBootstrap getBootstrap() {
        return bootstrap;
    }

    /**
     * 获取服务端配置
     *
     * @return 服务端配置
     */
    public ServerConfig getConfig() {
        return config;
    }

    /**
     * 创建 SocketIOServer 构建器
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建使用默认配置的 SocketIOServer
     *
     * @return SocketIOServer 实例
     */
    public static SocketIOServer createDefault() {
        return builder().build();
    }

    /**
     * SocketIOServer 构建器
     */
    public static class Builder {
        private ServerConfig config = ServerConfig.defaultConfig();

        /**
         * 设置服务端配置
         *
         * @param config 服务端配置
         * @return 当前构建器
         */
        public Builder config(ServerConfig config) {
            this.config = config;
            return this;
        }

        /**
         * 设置监听端口
         *
         * @param port 端口号
         * @return 当前构建器
         */
        public Builder port(int port) {
            this.config = ServerConfig.builder()
                    .port(port)
                    .build();
            return this;
        }

        /**
         * 设置 Ping 心跳间隔
         *
         * @param interval 心跳间隔（毫秒）
         * @return 当前构建器
         */
        public Builder pingInterval(long interval) {
            this.config = ServerConfig.builder()
                    .pingInterval(interval)
                    .build();
            return this;
        }

        /**
         * 设置 Ping 心跳超时
         *
         * @param timeout 心跳超时（毫秒）
         * @return 当前构建器
         */
        public Builder pingTimeout(long timeout) {
            this.config = ServerConfig.builder()
                    .pingTimeout(timeout)
                    .build();
            return this;
        }

        public SocketIOServer build() {
            return new SocketIOServer(config);
        }
    }
}
