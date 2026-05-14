package com.ccl.io.engine.netty.handler;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.EngineIOClient;
import io.netty.util.AttributeKey;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Netty Channel 属性键定义
 *
 * <p>用于在 Channel 的 AttributeMap 中存储和检索与 Engine.IO 会话相关的数据。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class ChannelAttributes {

    public static final AttributeKey<EngineClient> ENGINE_CLIENT = AttributeKey.valueOf("engine_client");
    /**
     * 会话 ID 属性键
     *
     * <p>存储当前 Channel 对应的 Engine.IO 会话唯一标识符。
     */
    public static final AttributeKey<String> SESSION_ID = AttributeKey.valueOf("sessionId");

    /**
     * HTTP 请求属性键
     *
     * <p>存储握手阶段的原始 HTTP 请求，用于后续处理流程中访问请求头等信息。
     */
    public static final AttributeKey<HttpRequest> HTTP_REQUEST = AttributeKey.valueOf("httpRequest");
}
