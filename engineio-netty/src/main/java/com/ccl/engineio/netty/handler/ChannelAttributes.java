package com.ccl.engineio.netty.handler;

import io.netty.util.AttributeKey;
import io.netty.handler.codec.http.HttpRequest;

public class ChannelAttributes {

    public static final AttributeKey<String> SESSION_ID = AttributeKey.valueOf("sessionId");
    public static final AttributeKey<HttpRequest> HTTP_REQUEST = AttributeKey.valueOf("httpRequest");

    private ChannelAttributes() {
    }
}
