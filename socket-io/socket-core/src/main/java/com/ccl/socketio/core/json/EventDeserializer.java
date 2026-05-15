package com.ccl.socketio.core.json;

import com.ccl.socketio.core.protocol.data.Event;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Socket.IO Event 自定义 JSON 反序列化器
 *
 * <p>将 Socket.IO 的事件数组格式 <code>["eventName", arg1, arg2, ...]</code>
 * 反序列化为 {@link Event} 对象。支持按声明参数类型进行类型化反序列化，
 * 无类型信息时回退为通用 Object 类型。
 *
 * @since 4.0.0
 */
public class EventDeserializer extends StdDeserializer<Event> {
    private final static Logger log = LoggerFactory.getLogger(EventDeserializer.class);

    private final List<Class<?>> eventClasses = new ArrayList<>();

    /**
     * 创建事件反序列化器（可变参数版本）
     *
     * @param clazz 事件参数类型列表
     */
    public EventDeserializer(Class<?>... clazz) {
       this(Arrays.asList(clazz));
    }

    /**
     * 创建事件反序列化器
     *
     * @param eventClasses 事件参数类型列表，用于类型化反序列化
     */
    public EventDeserializer(List<Class<?>> eventClasses) {
        super(Event.class);
        this.eventClasses.addAll(eventClasses);
    }

    /**
     * 反序列化 JSON 数组为 Event 对象
     *
     * <p>期望输入为 <code>["eventName", arg1, arg2, ...]</code> 格式的 JSON 数组。
     * 若注册了事件参数类型，则按类型反序列化；否则以通用 Object 类型处理。
     *
     * @param jp  JSON 解析器
     * @param ctx 反序列化上下文
     * @return 反序列化后的 Event 对象
     * @throws IOException 反序列化过程中可能抛出的 IO 异常
     */
    @Override
    public Event deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
        JsonToken token = jp.currentToken();
        if (token != JsonToken.START_ARRAY) {
            return null;
        }

        String eventName = jp.nextTextValue();
        ObjectCodec codec = jp.getCodec();

        if (!eventClasses.isEmpty()) {
            List<Object> eventArgs = new ArrayList<>();
            int i = 0;
            while (true) {
                token = jp.nextToken();
                if (token == JsonToken.END_ARRAY) {
                    break;
                }
                if (i > eventClasses.size() - 1) {
                    if (log.isWarnEnabled()) {
                        log.warn("Event {} has more args than declared in handler: {}", eventName, null);
                    }
                    break;
                }
                Class<?> eventClass = eventClasses.get(i);
                Object arg = codec.readValue(jp, eventClass);
                eventArgs.add(arg);
                i++;
            }
            return new Event(eventName, eventArgs);
        }

        List<Object> genericArgs = new ArrayList<>();
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            // read JSON into generic Object (Map/List/String/Number)
            Object value = codec.readValue(jp, Object.class);
            genericArgs.add(value);
        }

        return new Event(eventName, genericArgs);
    }
}
