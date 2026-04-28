package com.ccl.engineio.core;

import com.ccl.engineio.common.enums.EnginePacketType;
import com.ccl.engineio.common.enums.TransportType;
import com.ccl.engineio.core.entity.EnginePacket;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EnginePacketTest {

    @Test
    void testOpenPacket() {
        List<String> transports = Arrays.asList("websocket", "polling");
        EnginePacket packet = EnginePacket.openPacket("sid123", 25000, 20000, 1000000, transports);

        assertEquals(EnginePacketType.OPEN, packet.getType());
        assertNotNull(packet.getData());
        assertTrue(packet.getData().contains("sid123"));
        assertTrue(packet.getData().contains("\"upgrades\":"));
        assertTrue(packet.getData().contains("\"pingInterval\":25000"));
        assertTrue(packet.getData().contains("\"pingTimeout\":20000"));
        assertTrue(packet.getData().contains("\"maxPayload\":1000000"));
    }

    @Test
    void testPingPacket() {
        EnginePacket packet = EnginePacket.pingPacket();
        assertEquals(EnginePacketType.PING, packet.getType());
    }

    @Test
    void testPongPacket() {
        EnginePacket packet = EnginePacket.pongPacket();
        assertEquals(EnginePacketType.PONG, packet.getType());
    }

    @Test
    void testMessagePacket() {
        EnginePacket packet = EnginePacket.messagePacket("test message");
        assertEquals(EnginePacketType.MESSAGE, packet.getType());
        assertEquals("test message", packet.getData());
    }

    @Test
    void testClosePacket() {
        EnginePacket packet = EnginePacket.closePacket();
        assertEquals(EnginePacketType.CLOSE, packet.getType());
    }

    @Test
    void testUpgradePacket() {
        EnginePacket packet = EnginePacket.upgradePacket();
        assertEquals(EnginePacketType.UPGRADE, packet.getType());
    }

    @Test
    void testNoopPacket() {
        EnginePacket packet = EnginePacket.noopPacket();
        assertEquals(EnginePacketType.NOOP, packet.getType());
    }

    @Test
    void testIsBinaryWithBinaryData() {
        EnginePacket packet = new EnginePacket(EnginePacketType.MESSAGE, new byte[]{0x01, 0x02});
        assertTrue(packet.isBinary());
    }

    @Test
    void testIsBinaryWithNoBinaryData() {
        EnginePacket packet = new EnginePacket(EnginePacketType.MESSAGE, "text");
        assertFalse(packet.isBinary());
    }

    @Test
    void testTransportType() {
        EnginePacket packet = new EnginePacket(EnginePacketType.MESSAGE);
        packet.setTransportType(TransportType.WEBSOCKET);
        assertEquals(TransportType.WEBSOCKET, packet.getTransportType());
    }
}
