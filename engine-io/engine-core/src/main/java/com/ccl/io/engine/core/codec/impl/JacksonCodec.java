package com.ccl.io.engine.core.codec.impl;

import com.ccl.io.engine.codec.Codec;
import com.ccl.io.engine.exception.DeserializationException;
import com.ccl.io.engine.exception.SerializationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * 基于 Jackson 的字符串编解码器实现
 *
 * <p>使用 Jackson ObjectMapper 实现 JSON 序列化/反序列化能力，
 * 默认配置忽略空值、允许未知属性、大数字以明文输出</p>
 *
 * @author coffee377
 * @since 4.0.0
 */
public class JacksonCodec implements Codec {

    /**
     * Jackson 对象映射器，用于 JSON 处理
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 默认构造函数
     */
    public JacksonCodec() {
        this(new Module[]{});
    }

    /**
     * 带模块参数的构造函数
     *
     * @param modules 可选的 Jackson 模块数组
     */
    public JacksonCodec(Module... modules) {
        if (modules != null && modules.length > 0) {
            objectMapper.registerModules(modules);
        }
        init(objectMapper);
    }

    /**
     * 初始化 ObjectMapper 配置
     *
     * @param objectMapper 待配置的 ObjectMapper 实例
     */
    protected void init(ObjectMapper objectMapper) {
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public <T> T readValue(Reader reader, Class<T> clazz) throws DeserializationException {
        try {
            return objectMapper.readValue(reader, clazz);
        } catch (IOException e) {
            throw new DeserializationException(e.getMessage(), e);
        }
    }

    @Override
    public void writeValue(Writer writer, Object value) throws SerializationException {
        try {
            objectMapper.writeValue(writer, value);
        } catch (IOException e) {
            throw new SerializationException(e.getMessage(), e);
        }
    }

    @Override
    public <T> String serializeValueAsString(T value) throws SerializationException {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e.getMessage(), e);
        }
    }

    @Override
    public <T> byte[] serializeValueAsBytes(T value) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(String src, Class<T> clazz) throws DeserializationException {
        try {
            return objectMapper.readValue(src, clazz);
        } catch (JsonProcessingException e) {
            throw new DeserializationException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(byte[] src, Class<T> clazz) throws DeserializationException {
        try {
            return objectMapper.readValue(src, clazz);
        } catch (IOException e) {
            throw new DeserializationException(e.getMessage(), e);
        }
    }
}
