package com.ccl.io.engine.auth;

/**
 * 认证失败原因枚举
 *
 * @author coffee377
 * @since 4.0.0
 */
public enum AuthFailure {

    /**
     * 无效的认证令牌
     */
    INVALID_TOKEN("无效的认证令牌"),

    /**
     * 令牌已过期
     */
    EXPIRED_TOKEN("令牌已过期"),

    /**
     * 缺少认证信息
     */
    MISSING_TOKEN("缺少认证信息"),

    /**
     * 访问被拒绝
     */
    ACCESS_DENIED("访问被拒绝");

    private final String message;

    AuthFailure(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
