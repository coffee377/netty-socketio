package com.ccl.socketio.core;

import com.ccl.socketio.core.codec.SocketIODecoder;
import com.ccl.socketio.core.protocol.SocketPacketType;
import com.ccl.socketio.core.protocol.SocketPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketIODecoderTest {

    private SocketIODecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new SocketIODecoder();
    }

    @Test
    void testDecodeConnectPacket() {
        SocketPacket packet = decoder.decode("0");
        assertNotNull(packet);
        assertEquals(SocketPacketType.CONNECT, packet.getType());
    }

    @Test
    void testDecodeDisconnectPacket() {
        SocketPacket packet = decoder.decode("1");
        assertNotNull(packet);
        assertEquals(SocketPacketType.DISCONNECT, packet.getType());
    }

    @Test
    void testDecodeEventPacket() {
        SocketPacket packet = decoder.decode("2[\"chat\",{\"message\":\"hello\"}]");
        assertNotNull(packet);
        assertEquals(SocketPacketType.EVENT, packet.getType());
    }

    @Test
    void testDecodeAckPacket() {
        SocketPacket packet = decoder.decode("3[\"test\"]");
        assertNotNull(packet);
        assertEquals(SocketPacketType.ACK, packet.getType());
    }

    @Test
    void testDecodeErrorPacket() {
        SocketPacket packet = decoder.decode("4");
        assertNotNull(packet);
        assertEquals(SocketPacketType.ERROR, packet.getType());
    }

    @Test
    void testDecodeBinaryEventPacket() {
        SocketPacket packet = decoder.decode("5");
        assertNotNull(packet);
        assertEquals(SocketPacketType.BINARY_EVENT, packet.getType());
    }

    @Test
    void testDecodeBinaryAckPacket() {
        SocketPacket packet = decoder.decode("6");
        assertNotNull(packet);
        assertEquals(SocketPacketType.BINARY_ACK, packet.getType());
    }

    @Test
    void testDecodeWithNamespace() {
        SocketPacket packet = decoder.decode("0/chat");
        assertNotNull(packet);
        assertEquals(SocketPacketType.CONNECT, packet.getType());
    }

    @Test
    void testDecodeNullInput() {
        assertNull(decoder.decode(null));
    }

    @Test
    void testDecodeEmptyInput() {
        assertNull(decoder.decode(""));
    }
}
