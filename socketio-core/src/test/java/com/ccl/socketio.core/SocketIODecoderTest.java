package com.ccl.socketio.core;

import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.codec.impl.SocketIODecoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketIODecoderTest {

    private SocketDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new SocketIODecoderV5();
    }

    @Test
    void testDecodeConnectPacket() {
        SocketPacket<?> packet = decoder.decode("0");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.CONNECT, packet.getType());
    }

    @Test
    void testDecodeDisconnectPacket() {
        SocketPacket<?> packet = decoder.decode("1");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.DISCONNECT, packet.getType());
    }

    @Test
    void testDecodeEventPacket() {
        SocketPacket<?> packet = decoder.decode("2[\"chat\",{\"message\":\"hello\"}]");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.EVENT, packet.getType());
    }

    @Test
    void testDecodeAckPacket() {
        SocketPacket<?> packet = decoder.decode("3[\"test\"]");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.ACK, packet.getType());
    }

    @Test
    void testDecodeErrorPacket() {
        SocketPacket<?> packet = decoder.decode("4");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.ERROR, packet.getType());
    }

    @Test
    void testDecodeBinaryEventPacket() {
        SocketPacket<?> packet = decoder.decode("51-");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.BINARY_EVENT, packet.getType());
        assertEquals(1, packet.getAttachmentsCount());
    }

    @Test
    void testDecodeBinaryAckPacket() {
        SocketPacket<?> packet = decoder.decode("62-");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.BINARY_ACK, packet.getType());
        assertEquals(2, packet.getAttachmentsCount());
    }

    @Test
    void testDecodeWithNamespace() {
        SocketPacket<?> packet = decoder.decode("0/chat");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.CONNECT, packet.getType());
        assertEquals("/chat", packet.getNamespace());
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
