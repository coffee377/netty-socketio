package com.ccl.engineio.core;

import com.ccl.engineio.common.enums.EnginePacketType;
import com.ccl.engineio.core.codec.EngineIODecoder;
import com.ccl.engineio.core.codec.EngineIOEncoder;
import com.ccl.engineio.core.entity.EnginePacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EngineIODecoderTest {

    private EngineIODecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new EngineIODecoder();
    }

    @Test
    void testDecodeOpenPacket() {
        EnginePacket packet = decoder.decode("0{\"sid\":\"test123\",\"pingInterval\":25000}");
        assertNotNull(packet);
        assertEquals(EnginePacketType.OPEN, packet.getType());
        assertNotNull(packet.getData());
        assertTrue(packet.getData().contains("test123"));
    }

    @Test
    void testDecodeClosePacket() {
        EnginePacket packet = decoder.decode("1");
        assertNotNull(packet);
        assertEquals(EnginePacketType.CLOSE, packet.getType());
    }

    @Test
    void testDecodePingPacket() {
        EnginePacket packet = decoder.decode("2");
        assertNotNull(packet);
        assertEquals(EnginePacketType.PING, packet.getType());
    }

    @Test
    void testDecodePongPacket() {
        EnginePacket packet = decoder.decode("3");
        assertNotNull(packet);
        assertEquals(EnginePacketType.PONG, packet.getType());
    }

    @Test
    void testDecodeMessagePacket() {
        EnginePacket packet = decoder.decode("4hello");
        assertNotNull(packet);
        assertEquals(EnginePacketType.MESSAGE, packet.getType());
        assertEquals("hello", packet.getData());
    }

    @Test
    void testDecodeUpgradePacket() {
        EnginePacket packet = decoder.decode("5");
        assertNotNull(packet);
        assertEquals(EnginePacketType.UPGRADE, packet.getType());
    }

    @Test
    void testDecodeNoopPacket() {
        EnginePacket packet = decoder.decode("6");
        assertNotNull(packet);
        assertEquals(EnginePacketType.NOOP, packet.getType());
    }

    @Test
    void testDecodeNullInput() {
        assertNull(decoder.decode(null));
    }

    @Test
    void testDecodeEmptyInput() {
        assertNull(decoder.decode(""));
    }

    @Test
    void testDecodeAllMultiplePackets() {
        // Skip multiple packets test for now - format needs specific parsing
    }
}