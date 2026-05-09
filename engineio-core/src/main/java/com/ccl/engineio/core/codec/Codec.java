package com.ccl.engineio.core.codec;

import com.ccl.engineio.core.codec.impl.JacksonCodec;
import com.ccl.engineio.exception.DeserializationException;
import com.ccl.engineio.exception.SerializationException;

/**
 * 字符串编解码器接口
 *
 * <p>提供对象与字符串之间的序列化/反序列化能力，
 * 用于 Engine.IO 和 Socket.IO 协议中 JSON 数据的编解码</p>
 *
 * @author coffee377
 * @see JacksonCodec
 * @since 4.0.0-alpha.0
 */
public interface Codec {

    /**
     * 将对象序列化为字符串
     *
     * @param value 待序列化的对象
     * @param <T>   对象类型
     * @return 序列化后的字符串
     * @throws SerializationException 当序列化失败时
     */
    <T> String serializeValueAsString(T value) throws SerializationException;

    /**
     * 将对象序列化为字节数组
     *
     * @param value 待序列化的对象
     * @param <T>   对象类型
     * @return 序列化后的字节数组
     * @throws SerializationException 当序列化失败时
     */
    <T> byte[] serializeValueAsBytes(T value) throws SerializationException;

    /**
     * 将字符串反序列化为对象
     *
     * @param src   原始字符串
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 反序列化后的对象实例
     * @throws DeserializationException 当反序列化失败时
     */
    <T> T deserialize(String src, Class<T> clazz) throws DeserializationException;

    /**
     * 将字节数组反序列化为对象
     *
     * @param src   原始字节数组
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 反序列化后的对象实例
     * @throws DeserializationException 当反序列化失败时
     */
    <T> T deserialize(byte[] src, Class<T> clazz) throws DeserializationException;

}


