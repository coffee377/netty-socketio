package com.ccl.io.engine.core.entity;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 握手请求数据封装
 *
 * <p>包含客户端发起握手连接时的请求信息，包括客户端地址、请求 URL、
 * URL 参数、跨域标识及认证令牌等</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class HandshakeData {

    /**
     * 客户端地址
     */
    private InetSocketAddress address;

    /**
     * 握手时间戳
     */
    private final Instant time = Instant.now();

    /**
     * 服务端本地地址
     */
    private InetSocketAddress local;

    /**
     * 请求 URL
     */
    private String url;

    /**
     * URL 查询参数
     */
    private Map<String, List<String>> urlParams;

    /**
     * 跨域标识
     */
    private boolean xDomain;

    /**
     * 认证令牌
     */
    private Object authToken;

}
