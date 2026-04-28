package com.ccl.engineio.util;

import com.ccl.engineio.exception.EngineIOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new EngineIOException("Failed to serialize to JSON", e);
        }
    }

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
