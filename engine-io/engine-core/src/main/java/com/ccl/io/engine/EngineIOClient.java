package com.ccl.io.engine;

import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.Transport;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EngineIOClient<T> implements EngineClient<T> {

    private final int engineIOVersion;
    private final String sessionId;
    private final Transport transport;
    private final T handshakeData;
    private final AtomicBoolean connected;
    private final Queue<EngineIOPacket<?>> packets;

    private EngineIOClient(Builder<T> builder) {
        this.connected = builder.connected;
        this.engineIOVersion = builder.engineIOVersion;
        this.sessionId = builder.sessionId;
        this.transport = builder.transport;
        this.handshakeData = builder.handshakeData;
        this.packets = new ConcurrentLinkedQueue<>();
    }

    public static <D> Builder<D> builder() {
        return new Builder<>();
    }

    @Override
    public Queue<EngineIOPacket<?>> getEngineIOPacketQueue() {
        return packets;
    }

    @Override
    public int getEngineIOVersion() {
        return engineIOVersion;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public Transport getTransport() {
        return transport;
    }

    @Override
    public T getHandshakeData() {
        return handshakeData;
    }

    @Override
    public AtomicBoolean isConnected() {
        return connected;
    }

    @Override
    public <D> void send(EngineIOPacket<D> packet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(List<EngineIOPacket<?>> packets) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException();
    }

    public static class Builder<T> {
        private int engineIOVersion;
        private String sessionId;
        private Transport transport;
        private T handshakeData;
        private final AtomicBoolean connected = new AtomicBoolean(false);

        public Builder<T> engineIOVersion(int engineIOVersion) {
            this.engineIOVersion = engineIOVersion;
            return this;
        }

        public Builder<T> sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder<T> transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        public Builder<T> handshakeData(T handshakeData) {
            this.handshakeData = handshakeData;
            return this;
        }

        public Builder<T> connected(boolean connected) {
            this.connected.set(connected);
            return this;
        }

        public EngineClient<T> build() {
            if (engineIOVersion < 1) {
                engineIOVersion = 4;
            }
            return new EngineIOClient<>(this);
        }

    }
}
