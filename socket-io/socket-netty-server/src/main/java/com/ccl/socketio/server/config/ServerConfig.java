package com.ccl.socketio.server.config;

/**
 * Socket.IO 服务端配置
 *
 * <p>封装服务端启动所需的各项配置参数，通过 {@link ServerConfigBuilder} 构建。
 *
 * @author coffee377
 * @since 4.0.0
 */
public class ServerConfig {

    private final ServerOptions options;

    private ServerConfig(ServerOptions options) {
        this.options = options;
    }

    /**
     * 创建配置构建器
     *
     * @return 配置构建器实例
     */
    public static ServerConfigBuilder builder() {
        return new ServerConfigBuilder();
    }

    /**
     * 创建默认配置
     *
     * @return 默认服务端配置
     */
    public static ServerConfig defaultConfig() {
        return builder().build();
    }

    /**
     * 获取服务端选项
     *
     * @return 服务端选项对象
     */
    public ServerOptions getOptions() {
        return options;
    }

    /**
     * ServerConfig 构建器
     */
    public static class ServerConfigBuilder {
        private final ServerOptions options = new ServerOptions();

        /**
         * 设置监听端口
         *
         * @param port 端口号
         * @return 当前构建器
         */
        public ServerConfigBuilder port(int port) {
            options.setPort(port);
            return this;
        }

        /**
         * 设置 Ping 心跳间隔
         *
         * @param pingInterval 心跳间隔（毫秒）
         * @return 当前构建器
         */
        public ServerConfigBuilder pingInterval(long pingInterval) {
            options.setPingInterval(pingInterval);
            return this;
        }

        /**
         * 设置 Ping 心跳超时
         *
         * @param pingTimeout 心跳超时（毫秒）
         * @return 当前构建器
         */
        public ServerConfigBuilder pingTimeout(long pingTimeout) {
            options.setPingTimeout(pingTimeout);
            return this;
        }

        /**
         * 设置是否允许 CORS
         *
         * @param allowCors 是否允许 CORS
         * @return 当前构建器
         */
        public ServerConfigBuilder allowCors(boolean allowCors) {
            options.setAllowCors(allowCors);
            return this;
        }

        /**
         * 设置 CORS 允许的域名
         *
         * @param origin 允许的域名
         * @return 当前构建器
         */
        public ServerConfigBuilder corsOrigin(String origin) {
            options.setCorsOrigin(origin);
            return this;
        }

        /**
         * 设置启用的传输层
         *
         * @param transports 传输层列表
         * @return 当前构建器
         */
        public ServerConfigBuilder transports(java.util.List<String> transports) {
            options.setTransports(transports);
            return this;
        }

        /**
         * 设置是否启用 SSL
         *
         * @param enable 是否启用 SSL
         * @return 当前构建器
         */
        public ServerConfigBuilder enableSSL(boolean enable) {
            options.setEnableSSL(enable);
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(options);
        }
    }
}