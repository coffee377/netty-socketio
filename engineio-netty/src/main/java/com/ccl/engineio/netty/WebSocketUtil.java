package com.ccl.engineio.netty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * WebSocket 协议工具类
 *
 * <p>提供 WebSocket 握手所需的计算工具，包括 Sec-WebSocket-Accept 响应头的生成
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class WebSocketUtil {
    // WebSocket 协议固定 GUID
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /**
     * 计算 WebSocket 握手的 Sec-WebSocket-Accept
     */
    public static String calculateAccept(String clientKey) {
        if (clientKey == null || clientKey.isEmpty()) {
            throw new IllegalArgumentException("Sec-WebSocket-Key 不能为空");
        }

        try {
            // 1. 拼接 key + 固定GUID
            String key = clientKey + WEBSOCKET_GUID;
            // 2. SHA-1 哈希
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            // 3. Base64 编码
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            // JDK 必带 SHA-1，理论上不会触发
            throw new RuntimeException("不支持 SHA-1 算法", e);
        }
    }
}
