package com.ccl.socketio.server;

import com.ccl.socketio.netty.bootstrap.SocketIOBootstrap;
import com.ccl.socketio.server.config.ServerConfig;
import com.ccl.socketio.server.config.ServerOptions;
import com.ccl.socketio.server.handler.BusinessEventHandler;
import com.ccl.socketio.server.handler.GlobalExceptionHandler;
import com.ccl.socketio.server.listener.SocketIOListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void start() {
        try {
            bootstrap.start();
            log.info("Socket.IO server started on port {}", config.getOptions().getPort());
        } catch (InterruptedException e) {
            log.error("Failed to start server", e);
            Thread.currentThread().interrupt();
        }
    }


    public void stop() {
        bootstrap.stop();
        log.info("Socket.IO server stopped");
    }

    public SocketIOServer setListener(SocketIOListener listener) {
        this.listener = listener;
        bootstrap.setBusinessHandler(new BusinessEventHandler(listener));
        bootstrap.setGlobalExceptionHandler(new GlobalExceptionHandler());
        return this;
    }

    public void onEvent(String namespace, String eventName, java.util.function.Consumer<com.ccl.socketio.core.namespace.Namespace.SocketIOClient> handler) {
        bootstrap.setEventHandlerRegister(router -> {
            router.registerEventHandler(namespace, eventName, handler);
        });
    }

    public SocketIOListener getListener() {
        return listener;
    }

    public SocketIOBootstrap getBootstrap() {
        return bootstrap;
    }

    public ServerConfig getConfig() {
        return config;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SocketIOServer createDefault() {
        return builder().build();
    }

    public static class Builder {
        private ServerConfig config = ServerConfig.defaultConfig();

        public Builder config(ServerConfig config) {
            this.config = config;
            return this;
        }

        public Builder port(int port) {
            this.config = ServerConfig.builder()
                    .port(port)
                    .build();
            return this;
        }

        public Builder pingInterval(long interval) {
            this.config = ServerConfig.builder()
                    .pingInterval(interval)
                    .build();
            return this;
        }

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
