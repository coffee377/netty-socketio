package com.ccl.socketio.core.json;

import com.ccl.socketio.core.protocol.data.Event;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * {@link Event} 类型自定义序列化器
 *
 * <p>将 Event 序列化为 Socket.IO 协议要求的 JSON 数组格式：
 * {@code ["eventName", arg1, arg2, ...]}。
 * 如果参数中包含 byte[]，会自动触发 {@link ByteArraySerializer} 写入占位对象。
 *
 * @since 4.0.0
 */
public class EventSerializer extends JsonSerializer<Event> {

    @Override
    public void serialize(Event value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        gen.writeString(value.getName());
        if (value.getArgs() != null) {
            for (Object arg : value.getArgs()) {
                gen.writeObject(arg);
            }
        }
        gen.writeEndArray();
    }

}
