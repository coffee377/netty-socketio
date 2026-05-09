package com.ccl.engineio.core.codec;

import com.ccl.engineio.exception.DeserializationException;
import com.ccl.engineio.exception.SerializationException;

/**
 * 字符串编解码器接口
 *
 * <p>提供对象与字符串之间的序列化/反序列化能力，
 * 用于 Engine.IO 和 Socket.IO 协议中 JSON 数据的编解码</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 * @see com.ccl.engineio.core.codec.impl.JacksonStringCodec
 */
public interface StringCodec {

    /**
     * 将对象序列化为字符串
     *
     * @param obj 待序列化的对象
     * @param <T> 对象类型
     * @return 序列化后的字符串
     * @throws SerializationException 当序列化失败时
     */
    <T> String serialize(T obj) throws SerializationException;

    /**
     * 将字符串反序列化为对象
     *
     * @param str   原始字符串
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 反序列化后的对象实例
     * @throws DeserializationException 当反序列化失败时
     */
    <T> T deserialize(String str, Class<T> clazz) throws DeserializationException;

}


