/**
 * Copyright (c) 2025 The Socketio4j Project
 * Parent project : Copyright (c) 2012-2025 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.socketio4j.socketio.protocol;

import java.io.IOException;
import java.util.List;

import com.socketio4j.socketio.AckCallback;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

/**
 * JSON 序列化支持接口
 *
 * <p>允许自定义 JSON 序列化与反序列化的实现，
 * 用于处理 Socket.IO 协议中的数据包编解码
 */
public interface JsonSupport {

    /**
     * 从输入流读取确认回调参数
     *
     * @param src      输入流
     * @param callback 确认回调
     * @return 确认参数
     * @throws IOException 读取失败时抛出
     */
    AckArgs readAckArgs(ByteBufInputStream src, AckCallback<?> callback) throws IOException;

    /**
     * 从输入流读取指定类型的值
     *
     * @param <T>           值类型
     * @param namespaceName 命名空间名称
     * @param src           输入流
     * @param valueType     目标类型
     * @return 反序列化后的值
     * @throws IOException 读取失败时抛出
     */
    <T> T readValue(String namespaceName, ByteBufInputStream src, Class<T> valueType) throws IOException;

    /**
     * 将对象写入输出流
     *
     * @param out   输出流
     * @param value 待写入的对象
     * @throws IOException 写入失败时抛出
     */
    void writeValue(ByteBufOutputStream out, Object value) throws IOException;

    /**
     * 添加事件类型映射
     *
     * @param namespaceName 命名空间名称
     * @param eventName     事件名称
     * @param eventClass    事件数据类型
     */
    void addEventMapping(String namespaceName, String eventName, Class<?>... eventClass);

    /**
     * 移除事件类型映射
     *
     * @param namespaceName 命名空间名称
     * @param eventName     事件名称
     */
    void removeEventMapping(String namespaceName, String eventName);

    /**
     * 获取二进制数据数组列表
     *
     * @return 二进制数据数组列表
     */
    List<byte[]> getArrays();

}
