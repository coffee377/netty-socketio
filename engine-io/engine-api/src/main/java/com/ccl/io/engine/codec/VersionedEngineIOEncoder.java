package com.ccl.io.engine.codec;

import com.ccl.io.engine.protocol.EngineIOPacket;

import java.nio.ByteBuffer;
import java.util.List;

public interface VersionedEngineIOEncoder extends EngineIOEncoder {

    default byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary, int protocolVersion) {
        if (!isSupport(protocolVersion)) return null;
        return encodePacket(packet, supportBinary);
    }

    default ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary, int protocolVersion) {
        if (!isSupport(protocolVersion)) return null;
        return encodePayload(packets, supportBinary);
    }

}
