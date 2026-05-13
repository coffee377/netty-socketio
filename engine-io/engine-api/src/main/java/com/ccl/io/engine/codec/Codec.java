package com.ccl.io.engine.codec;

import com.ccl.io.engine.exception.DeserializationException;
import com.ccl.io.engine.exception.NoImplementationException;
import com.ccl.io.engine.exception.SerializationException;

import java.io.Reader;
import java.io.Writer;

public interface Codec {

    Codec NOOP = new NoOpCodec();

    /**
     * 从 Reader 读取并反序列化对象
     *
     * @param reader 数据源
     * @param clazz  目标类型
     * @param <T>    目标类型
     * @return 反序列化后的对象
     * @throws DeserializationException 当读取或反序列化失败时
     */
    default <T> T readValue(Reader reader, Class<T> clazz) throws DeserializationException {
        throw new NoImplementationException();
    }

    /**
     * 将对象序列化写入 Writer
     *
     * @param writer 写入目标
     * @param value  待序列化的对象
     * @throws SerializationException 当序列化或写入失败时
     */
    default void writeValue(Writer writer, Object value) throws SerializationException {
        throw new NoImplementationException();
    }

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


