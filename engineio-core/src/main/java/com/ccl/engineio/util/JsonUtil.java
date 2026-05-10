package com.ccl.engineio.util;

import com.ccl.engineio.exception.EngineIOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 *
 * <p>提供基于 Jackson 的 JSON 序列化与反序列化快捷操作</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private JsonUtil() {
    }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 待序列化的对象
     * @return JSON 格式字符串
     * @throws EngineIOException 当序列化失败时
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new EngineIOException("Failed to serialize to JSON", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 反序列化后的对象实例
     * @throws EngineIOException 当反序列化失败时
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new EngineIOException("Failed to deserialize from JSON", e);
        }
    }

//    public static Map<String, Object> toMap(String json) {
//        return fromJson(json, Map.class);
//    }
//
//    public static List<Object> toList(String json) {
//        return fromJson(json, List.class);
//    }
//
//    public static Map<String, Object> toMapOrEmpty(String json) {
//        if (json == null || json.isEmpty()) {
//            return Collections.emptyMap();
//        }
//        try {
//            return MAPPER.readValue(json, Map.class);
//        } catch (IOException e) {
//            return Collections.emptyMap();
//        }
//    }
}
