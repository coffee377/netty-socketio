package com.ccl.io.engine.auth;

import com.ccl.io.engine.Handshake;

/**
 * 异步认证接口
 *
 * @author coffee377
 * @since 4.0.0
 */
public interface AsyncAuthenticator {

    /**
     * 执行异步认证
     *
     * @param handshake 握手数据
     * @param callback  认证结果回调
     */
    void authenticate(Handshake handshake, AuthCallback callback);

    /**
     * 认证结果回调接口
     */
    interface AuthCallback {

        /**
         * 接收认证结果
         *
         * @param result 认证结果
         */
        void onResult(AuthResult result);
    }
}
