package com.ccl.socketio.server.config;

public class ServerConfig {

    private final ServerOptions options;

    private ServerConfig(ServerOptions options) {
        this.options = options;
    }

    public static ServerConfigBuilder builder() {
        return new ServerConfigBuilder();
    }

    public static ServerConfig defaultConfig() {
        return builder().build();
    }

    public ServerOptions getOptions() {
        return options;
    }

    public static class ServerConfigBuilder {
        private final ServerOptions options = new ServerOptions();

        public ServerConfigBuilder port(int port) {
            options.setPort(port);
            return this;
        }

        public ServerConfigBuilder pingInterval(long pingInterval) {
            options.setPingInterval(pingInterval);
            return this;
        }

        public ServerConfigBuilder pingTimeout(long pingTimeout) {
            options.setPingTimeout(pingTimeout);
            return this;
        }

        public ServerConfigBuilder allowCors(boolean allowCors) {
            options.setAllowCors(allowCors);
            return this;
        }

        public ServerConfigBuilder corsOrigin(String origin) {
            options.setCorsOrigin(origin);
            return this;
        }

        public ServerConfigBuilder transports(java.util.List<String> transports) {
            options.setTransports(transports);
            return this;
        }

        public ServerConfigBuilder enableSSL(boolean enable) {
            options.setEnableSSL(enable);
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(options);
        }
    }
}