package com.ccl.io.engine.codec;

import com.ccl.io.engine.protocol.EngineIOPacket;

import java.util.List;

public interface VersionedEngineIODecoder extends EngineIODecoder {

    default EngineIOPacket<?> decodePacket(Object data, int protocolVersion) {
        if (!isSupport(protocolVersion)) return null;
        return decodePacket(data);
    }


    default List<EngineIOPacket<?>> decodePayload(Object payload, int protocolVersion) {
        if (!isSupport(protocolVersion)) return null;
        return decodePayload(payload);
    }

}
