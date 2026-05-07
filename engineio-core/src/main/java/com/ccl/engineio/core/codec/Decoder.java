package com.ccl.engineio.core.codec;


import com.ccl.engineio.core.protocol.DataType;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.EngineVersion;

import java.util.List;

public interface Decoder {

    boolean support(EngineVersion version);

    /**
     * 解码单个数据包
     *
     * @param data     原始数据（字符串或字节数组）
     * @param dataType 数据类型
     * @return 解码后的数据包，data 为 null 时返回 null
     */
    EngineIOPacket<?> decodePacket(Object data, DataType dataType);

    /**
     * 解码多个数据包（Payload）
     *
     * @param payload     原始数据
     * @param dataType 数据类型
     * @return 解码后的数据包列表
     */
    List<EngineIOPacket<?>> decodePayload(Object payload, DataType dataType);

}
