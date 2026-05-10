package com.ccl.engineio.netty.handler;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;

/**
 * CORS 跨域资源共享工具类
 *
 * <p>提供 CORS 响应头的统一设置方法，支持两种模式：
 * <ul>
 *   <li>回显模式：根据请求的 Origin 头动态设置 Allow-Origin</li>
 *   <li>固定模式：使用预配置的允许来源列表</li>
 * </ul>
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public final class CorsUtil {

    private CorsUtil() {
    }

    /**
     * 回显模式：根据请求的 Origin 设置 CORS 头
     *
     * <p>当 origin 非空时，将其设为 Allow-Origin 并允许携带凭据；
     * 否则默认允许所有来源（*）。
     *
     * @param response HTTP 响应
     * @param origin   请求的 Origin 头值
     */
    public static void addCorsHeaders(HttpResponse response, String origin) {
        if (origin != null) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
        } else {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }
    }

    /**
     * 固定模式：使用预配置的允许来源设置 CORS 头
     *
     * <p>仅在 enableCors 为 true 时添加 CORS 头。
     * 当 corsOrigin 为 "*" 时允许所有来源且不设置凭据；
     * 否则仅允许指定来源并允许携带凭据。
     *
     * @param response   HTTP 响应
     * @param corsOrigin 允许的来源
     * @param enableCors 是否启用 CORS
     */
    public static void addCorsHeaders(HttpResponse response, String corsOrigin, boolean enableCors) {
        if (!enableCors) return;
        if (corsOrigin != null && !corsOrigin.isEmpty()) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, corsOrigin);
            if (!"*".equals(corsOrigin)) {
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
            }
        }
    }
}
