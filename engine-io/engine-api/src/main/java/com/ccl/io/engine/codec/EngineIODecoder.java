package com.ccl.io.engine.codec;

import com.ccl.io.engine.EngineIO;
import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.EngineIOVersion;

import java.util.List;

/**
 * Engine.IO 协议解码器接口
 *
 * <p>负责将原始数据解码为 {@link EngineIOPacket} 数据包实例，
 * 支持单个数据包解码和批量 Payload 解码两种模式</p>
 *
 * @author coffee377
 * @see EngineIOEncoder
 * @since 4.0.0
 */
public interface EngineIODecoder extends EngineIO {

    /**
     * 解码单个数据包（使用默认协议版本）
     *
     * <p>默认委托到 {@link #decodePacket(Object, int)}，使用 V4 版本</p>
     *
     * @param data 原始数据（字符串或字节数组）
     * @return 解码后的数据包，data 为 null 时返回 null
     */
    default EngineIOPacket<?> decodePacket(Object data) {
        return decodePacket(data, EngineIOVersion.V4.getValue());
    }

    /**
     * 解码指定协议版本的单个数据包
     *
     * <p>将原始数据解码为 {@link EngineIOPacket}，根据数据包类型和内容构建对应的数据包实例</p>
     *
     * @param data            原始数据（字符串或字节数组）
     * @param protocolVersion  Engine.IO 协议版本号
     * @return 解码后的数据包，data 为 null 时返回 null
     */
    EngineIOPacket<?> decodePacket(Object data, int protocolVersion);

    /**
     * 解码多个数据包（Payload，使用默认协议版本）
     *
     * <p>默认委托到 {@link #decodePayload(Object, int)}，使用 V4 版本</p>
     *
     * @param payload 原始数据（字符串或字节数组）
     * @return 解码后的数据包列表，空数据返回空列表
     */
    default List<EngineIOPacket<?>> decodePayload(Object payload) {
        return decodePayload(payload, EngineIOVersion.V4.getValue());
    }

    /**
     * 解码指定协议版本的批量 Payload
     *
     * <p>将包含多个数据包的 Payload 数据解码为数据包列表，
     * 使用记录分隔符（如 0x1E）分割各个数据包</p>
     *
     * @param payload         原始数据（字符串或字节数组）
     * @param protocolVersion Engine.IO 协议版本号
     * @return 解码后的数据包列表，空数据返回空列表
     */
    List<EngineIOPacket<?>> decodePayload(Object payload, int protocolVersion);

}
