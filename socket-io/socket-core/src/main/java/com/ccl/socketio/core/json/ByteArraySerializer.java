package com.ccl.socketio.core.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * byte[] 类型自定义序列化器
 *
 * <p>在序列化 byte[] 时，不直接输出二进制数据（如 Base64），
 * 而是写入 {@code {"_placeholder":true,"num":N}} 占位对象，
 * 同时将原始 byte[] 捕获到 {@link #BINARY_DATA} 中供后续处理。
 * 这是 Socket.IO 协议中二进制附件的标准处理方式。
 *
 * @since 4.0.0
 */
public class ByteArraySerializer extends JsonSerializer<byte[]> {

    /**
     * 序列化过程中捕获的 byte[] 数据
     *
     * <p>每个序列化操作会按遇到 byte[] 的顺序，将原始数据追加到此列表中。
     * 序列化完成后应调用 {@link #clear()} 清理状态，防止内存泄漏。
     * 使用 ThreadLocal 保证线程安全。
     */
    public static final ThreadLocal<List<byte[]>> BINARY_DATA = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        List<byte[]> binaries = BINARY_DATA.get();
        int num = binaries.size();
        gen.writeStartObject();
        gen.writeBooleanField("_placeholder", true);
        gen.writeNumberField("num", num);
        gen.writeEndObject();
        binaries.add(value);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, byte[] value) {
        return super.isEmpty(provider, value) || value.length == 0;
    }

    /**
     * 获取本次序列化过程中捕获的全部 byte[] 数据
     *
     * @return byte[] 列表，按序列化顺序排列
     */
    public static List<byte[]> getArrays() {
        return BINARY_DATA.get();
    }

    /**
     * 清理序列化过程中捕获的 byte[] 数据
     *
     * <p>在每次序列化操作完成后调用，避免 ThreadLocal 中累积数据导致内存泄漏。
     */
    public static void clear() {
        BINARY_DATA.get().clear();
    }

}
