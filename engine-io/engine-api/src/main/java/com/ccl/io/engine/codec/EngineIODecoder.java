package com.ccl.io.engine.codec;

import com.ccl.io.engine.EngineIO;
import com.ccl.io.engine.exception.EngineIOException;
import com.ccl.io.engine.protocol.EngineIOPacket;

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
     * 解码单个数据包
     *
     * <p>将原始数据（字符串或字节数组）解码为 {@link EngineIOPacket}，
     * 根据数据包类型和内容构建对应的数据包实例</p>
     *
     * @param data 原始数据（字符串或字节数组）
     * @return 解码后的数据包，data 为 null 时返回 null
     */
    EngineIOPacket<?> decodePacket(Object data);

    /**
     * 解码多个数据包（Payload）
     *
     * <p>将包含多个数据包的 Payload 数据解码为数据包列表，
     * 使用记录分隔符（如 0x1E）分割各个数据包</p>
     *
     * @param payload 原始数据（字符串或字节数组）
     * @return 解码后的数据包列表，空数据返回空列表
     */
    List<EngineIOPacket<?>> decodePayload(Object payload);

    /**
     * 解码指定协议版本的数据包
     *
     * <p>委托到 {@link #decodePacket(Object)} 前先校验版本兼容性</p>
     *
     * @param data            原始数据（字符串或字节数组）
     * @param protocolVersion Engine.IO 协议版本号
     * @return 解码后的数据包，data 为 null 时返回 null
     * @throws EngineIOException 当该实现不支持指定的协议版本时
     */
    default EngineIOPacket<?> decodePacket(Object data, int protocolVersion) {
        if (!isSupport(protocolVersion)) {
            throw new EngineIOException("Unsupported Engine.IO protocol version: " + protocolVersion);
        }
        return decodePacket(data);
    }

    /**
     * 解码指定协议版本的批量 Payload
     *
     * <p>委托到 {@link #decodePayload(Object)} 前先校验版本兼容性</p>
     *
     * @param payload         原始数据（字符串或字节数组）
     * @param protocolVersion Engine.IO 协议版本号
     * @return 解码后的数据包列表，空数据返回空列表
     * @throws EngineIOException 当该实现不支持指定的协议版本时
     */
    default List<EngineIOPacket<?>> decodePayload(Object payload, int protocolVersion) {
        if (!isSupport(protocolVersion)) {
            throw new EngineIOException("Unsupported Engine.IO protocol version: " + protocolVersion);
        }
        return decodePayload(payload);
    }

}
