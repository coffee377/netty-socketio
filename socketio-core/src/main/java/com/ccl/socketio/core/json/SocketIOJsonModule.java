package com.ccl.socketio.core.json;

import com.ccl.socketio.core.protocol.data.Event;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Socket.IO 自定义 JSON 序列化模块
 *
 * <p>注册 Socket.IO 协议所需的 Jackson 自定义序列化器：
 * <ul>
 *   <li>{@link EventSerializer} — 将 Event 序列化为 JSON 数组格式</li>
 *   <li>{@link ByteArraySerializer} — 将 byte[] 序列化为二进制占位对象</li>
 * </ul>
 *
 * <p>使用时传入 {@code JacksonCodec} 构造函数即可生效：
 * <pre>{@code
 * new JacksonCodec(new SocketIOJsonModule())
 * }</pre>
 *
 * @since 4.0.0-alpha.0
 */
public class SocketIOJsonModule extends SimpleModule {

    public SocketIOJsonModule() {
        addSerializer(Event.class, new EventSerializer());
        addSerializer(byte[].class, new ByteArraySerializer());
    }

}
