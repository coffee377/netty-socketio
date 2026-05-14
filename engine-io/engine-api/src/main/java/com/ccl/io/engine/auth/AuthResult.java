package com.ccl.io.engine.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证结果接口
 *
 * @author coffee377
 * @since 4.0.0
 */
public interface AuthResult {

    /**
     * 创建成功结果
     *
     * @return 成功结果
     */
    static AuthResult success() {
        return new SimpleAuthResult(true, null, Collections.emptyMap());
    }

    /**
     * 创建成功结果，带用户属性
     *
     * @param attributes 用户属性
     * @return 成功结果
     */
    static AuthResult success(Map<String, Object> attributes) {
        return new SimpleAuthResult(true, null, attributes);
    }

    /**
     * 创建失败结果
     *
     * @param failure 失败原因
     * @return 失败结果
     */
    static AuthResult failure(AuthFailure failure) {
        return new SimpleAuthResult(false, failure.getMessage(), Collections.emptyMap());
    }

    /**
     * 创建失败结果
     *
     * @param reason 失败原因描述
     * @return 失败结果
     */
    static AuthResult failure(String reason) {
        return new SimpleAuthResult(false, reason, Collections.emptyMap());
    }

    /**
     * 是否认证通过
     *
     * @return true 表示认证通过
     */
    boolean isAuthorized();

    /**
     * 获取失败原因
     *
     * @return 失败原因描述，认证成功时返回 null
     */
    String getReason();

    /**
     * 获取用户属性
     *
     * <p>包含用户 ID、角色等认证后需要传递给业务层的信息</p>
     *
     * @return 用户属性映射表
     */
    Map<String, Object> getAttributes();

    /**
     * 简单认证结果实现
     */
    class SimpleAuthResult implements AuthResult {

        private final boolean authorized;
        private final String reason;
        private final Map<String, Object> attributes;

        public SimpleAuthResult(boolean authorized, String reason, Map<String, Object> attributes) {
            this.authorized = authorized;
            this.reason = reason;
            this.attributes = new HashMap<>(attributes);
        }

        @Override
        public boolean isAuthorized() {
            return authorized;
        }

        @Override
        public String getReason() {
            return reason;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.unmodifiableMap(attributes);
        }
    }
}
