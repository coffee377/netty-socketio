package com.ccl.io.engine;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 握手请求数据封装
 *
 * <p>包含客户端发起握手连接时的请求信息，包括客户端地址、请求 URL、
 * URL 参数、跨域标识等</p>
 *
 * @author coffee377
 * @since 4.0.0
 */
public class Handshake {

    private final Instant time = Instant.now();
    private final InetSocketAddress remoteAddress;
    private final InetSocketAddress localAddress;
    private final String url;
    private final Map<String, List<String>> urlParams;
    private final boolean xDomain;
    private final Object authToken;

    private Handshake(Builder builder) {
        this.remoteAddress = builder.remoteAddress;
        this.localAddress = builder.localAddress;
        this.url = builder.url;
        this.urlParams = builder.urlParams;
        this.xDomain = builder.xDomain;
        this.authToken = builder.authToken;
    }

    /**
     * 创建 Builder 实例
     *
     * @return 新的 Builder 对象
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取握手时间戳
     *
     * @return 握手发生的时间
     */
    public Instant getTime() {
        return time;
    }

    /**
     * 获取客户端地址
     *
     * @return 客户端的远程地址
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * 获取服务端本地地址
     *
     * @return 服务端监听的地址
     */
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * 获取请求 URL
     *
     * @return 请求的完整路径
     */
    public String getUrl() {
        return url;
    }

    /**
     * 获取 URL 查询参数
     *
     * @return 参数映射表
     */
    public Map<String, List<String>> getUrlParams() {
        return urlParams;
    }

    /**
     * 获取跨域标识
     *
     * @return 是否为跨域请求
     */
    public boolean isXDomain() {
        return xDomain;
    }

    /**
     * 获取认证令牌
     *
     * @return 认证信息对象
     */
    public Object getAuthToken() {
        return authToken;
    }

    /**
     * Handshake 对象构建器
     *
     * <p>提供链式调用方式构造 Handshake 实例</p>
     */
    public static class Builder {

        private InetSocketAddress remoteAddress;
        private InetSocketAddress localAddress;
        private String url;
        private Map<String, List<String>> urlParams;
        private boolean xDomain;
        private Object authToken;

        /**
         * 设置客户端地址
         *
         * @param remoteAddress 客户端的远程地址
         * @return this
         */
        public Builder remoteAddress(InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        /**
         * 设置服务端本地地址
         *
         * @param localAddress 服务端监听的地址
         * @return this
         */
        public Builder localAddress(InetSocketAddress localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        /**
         * 设置请求 URL
         *
         * @param url 请求的完整路径
         * @return this
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * 设置 URL 查询参数
         *
         * @param urlParams 参数映射表
         * @return this
         */
        public Builder urlParams(Map<String, List<String>> urlParams) {
            this.urlParams = urlParams;
            return this;
        }

        /**
         * 设置跨域标识
         *
         * @param xDomain 是否为跨域请求
         * @return this
         */
        public Builder xDomain(boolean xDomain) {
            this.xDomain = xDomain;
            return this;
        }

        /**
         * 设置认证令牌
         *
         * @param authToken 认证信息对象
         * @return this
         */
        public Builder authToken(Object authToken) {
            this.authToken = authToken;
            return this;
        }

        /**
         * 构建 Handshake 实例
         *
         * @return 新的 Handshake 对象
         */
        public Handshake build() {
            return new Handshake(this);
        }
    }
}
