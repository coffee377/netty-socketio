package com.ccl.engineio.core.codec.impl;

import com.ccl.engineio.core.codec.StringCodec;
import com.ccl.engineio.exception.DeserializationException;
import com.ccl.engineio.exception.SerializationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

/**
 * 基于 Jackson 的字符串编解码器实现
 *
 * <p>使用 Jackson ObjectMapper 实现 JSON 序列化/反序列化能力，
 * 默认配置忽略空值、允许未知属性、大数字以明文输出</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class JacksonStringCodec implements StringCodec {

    /**
     * Jackson 对象映射器，用于 JSON 处理
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 默认构造函数
     */
    public JacksonStringCodec() {
        this(new Module[] {});
    }

    /**
     * 带模块参数的构造函数
     *
     * @param modules 可选的 Jackson 模块数组
     */
    public JacksonStringCodec(Module... modules) {
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
    public <T> String serialize(T obj) throws SerializationException {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(String str, Class<T> clazz) throws DeserializationException {
        try {
            return objectMapper.readValue(str, clazz);
        } catch (JsonProcessingException e) {
            throw new DeserializationException(e.getMessage(), e);
        }
    }

}
