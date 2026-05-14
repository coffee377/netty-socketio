package com.ccl.io.engine;

import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.Transport;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EngineIOClient implements EngineClient {

    private final int engineIOVersion;
    private final String sessionId;
    private final Transport transport;
    private final Handshake handshakeData;
    private final AtomicBoolean connected;
    private final Queue<EngineIOPacket<?>> packets;

    private EngineIOClient(Builder builder) {
        this.connected = builder.connected;
        this.engineIOVersion = builder.engineIOVersion;
        this.sessionId = builder.sessionId;
        this.transport = builder.transport;
        this.handshakeData = builder.handshakeData;
        this.packets = new ConcurrentLinkedQueue<>();
    }

    public static Builder builder() {
        return new Builder();
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
    public Handshake getHandshakeData() {
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

    public static class Builder {
        private String sessionId;
        private Integer engineIOVersion;
        private Transport transport;
        private Handshake handshakeData;
        private final AtomicBoolean connected = new AtomicBoolean(false);

        public Builder engineIOVersion(Integer engineIOVersion) {
            this.engineIOVersion = engineIOVersion;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        public Builder handshakeData(Handshake handshakeData) {
            this.handshakeData = handshakeData;
            return this;
        }

        public Builder connected(boolean connected) {
            this.connected.set(connected);
            return this;
        }

        public EngineClient build() {
            if (engineIOVersion == null || engineIOVersion < 0) {
                engineIOVersion = 4;
            }
            return new EngineIOClient(this);
        }

    }
}
