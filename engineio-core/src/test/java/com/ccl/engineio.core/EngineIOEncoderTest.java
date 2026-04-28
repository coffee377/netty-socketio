package com.ccl.engineio.core;

import com.ccl.engineio.common.enums.EnginePacketType;
import com.ccl.engineio.core.codec.EngineIOEncoder;
import com.ccl.engineio.core.entity.EnginePacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EngineIOEncoderTest {

    private EngineIOEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new EngineIOEncoder();
    }

    @Test
    void testEncodeOpenPacket() {
        EnginePacket packet = new EnginePacket(EnginePacketType.OPEN, "test data");
        String encoded = encoder.encode(packet);
        assertEquals("0test data", encoded);
    }

    @Test
    void testEncodeClosePacket() {
        EnginePacket packet = new EnginePacket(EnginePacketType.CLOSE);
        String encoded = encoder.encode(packet);
        assertEquals("1", encoded);
    }

    @Test
    void testEncodePingPacket() {
        EnginePacket packet = new EnginePacket(EnginePacketType.PING);
        String encoded = encoder.encode(packet);
        assertEquals("2", encoded);
    }

    @Test
    void testEncodePongPacket() {
        EnginePacket packet = new EnginePacket(EnginePacketType.PONG);
        String encoded = encoder.encode(packet);
        assertEquals("3", encoded);
    }

    @Test
    void testEncodeMessagePacket() {
        EnginePacket packet = new EnginePacket(EnginePacketType.MESSAGE, "hello world");
        String encoded = encoder.encode(packet);
        assertEquals("4hello world", encoded);
    }

    @Test
    void testEncodeUpgradePacket() {
        EnginePacket packet = new EnginePacket(EnginePacketType.UPGRADE);
        String encoded = encoder.encode(packet);
        assertEquals("5", encoded);
    }

    @Test
    void testEncodeNoopPacket() {
        EnginePacket packet = new EnginePacket(EnginePacketType.NOOP);
        String encoded = encoder.encode(packet);
        assertEquals("6", encoded);
    }

    @Test
    void testEncodeNullPacket() {
        String encoded = encoder.encode(null);
        assertEquals("", encoded);
    }

    @Test
    void testEncodePacketWithNullData() {
        EnginePacket packet = new EnginePacket(EnginePacketType.MESSAGE);
        String encoded = encoder.encode(packet);
        assertEquals("4", encoded);
    }

    @Test
    void testEncodeMultiplePackets() {
        EnginePacket packet1 = EnginePacket.closePacket();
        EnginePacket packet2 = EnginePacket.pingPacket();
        String encoded = encoder.encodeMultiple(packet1, packet2);
        assertTrue(encoded.contains("1"));
        assertTrue(encoded.contains("2"));
    }

    @Test
    void testEncodeBinaryPacket() {
        EnginePacket packet = new EnginePacket(EnginePacketType.MESSAGE, new byte[]{0x01, 0x02, 0x03});
        byte[] encoded = encoder.encodeBinary(packet);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }
}