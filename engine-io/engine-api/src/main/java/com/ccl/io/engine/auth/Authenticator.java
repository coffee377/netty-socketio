package com.ccl.io.engine.auth;

import com.ccl.io.engine.Handshake;
import org.jetbrains.annotations.NotNull;

/**
 * 同步认证接口
 *
 * @author coffee377
 * @since 4.0.0
 */
@FunctionalInterface
public interface Authenticator {

    Authenticator NOOP = handshake -> AuthResult.success();

    /**
     * 执行认证
     *
     * @param handshake 握手数据
     * @return 认证结果
     */
    AuthResult authenticate(@NotNull Handshake handshake);
}
