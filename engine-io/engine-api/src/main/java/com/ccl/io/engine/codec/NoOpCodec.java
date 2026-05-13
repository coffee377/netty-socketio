package com.ccl.io.engine.codec;

import com.ccl.io.engine.exception.DeserializationException;
import com.ccl.io.engine.exception.NoImplementationException;
import com.ccl.io.engine.exception.SerializationException;

class NoOpCodec implements Codec {

    @Override
    public <T> String serializeValueAsString(T value) throws SerializationException {
        throw new NoImplementationException();
    }

    @Override
    public <T> byte[] serializeValueAsBytes(T value) throws SerializationException {
        throw new NoImplementationException();
    }

    @Override
    public <T> T deserialize(String src, Class<T> clazz) throws DeserializationException {
        throw new NoImplementationException();
    }

    @Override
    public <T> T deserialize(byte[] src, Class<T> clazz) throws DeserializationException {
        throw new NoImplementationException();
    }
}
