package com.ccl.io.engine;

import com.ccl.io.engine.exception.NoImplementationException;
import com.ccl.io.engine.protocol.EngineIOPacket;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

class NoOpParser implements Parser {
    @Override
    public EngineIOPacket<?> decodePacket(Object data) {
        throw new NoImplementationException();
    }

    @Override
    public List<EngineIOPacket<?>> decodePayload(Object payload) {
        throw new NoImplementationException();
    }

    @Override
    public byte[] encodePacket(EngineIOPacket<?> packet, boolean supportBinary) {
        throw new NoImplementationException();
    }

    @Override
    public ByteBuffer encodePayload(List<EngineIOPacket<?>> packets, boolean supportBinary) {
        throw new NoImplementationException();
    }
}
