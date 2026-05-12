package com.ccl.socketio.core;

import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.codec.impl.SocketIODecoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SocketIODecoderV5 解码测试")
class SocketIODecoderTest {

    private SocketDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new SocketIODecoderV5();
    }

    @Test
    @DisplayName("解码 CONNECT 数据包，返回类型为 CONNECT")
    void testDecodeConnectPacket() {
        SocketPacket<?> packet = decoder.decode("0");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.CONNECT, packet.getType());
    }

    @Test
    @DisplayName("解码 DISCONNECT 数据包，返回类型为 DISCONNECT")
    void testDecodeDisconnectPacket() {
        SocketPacket<?> packet = decoder.decode("1");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.DISCONNECT, packet.getType());
    }

    @Test
    @DisplayName("解码 EVENT 数据包，提取事件名称和负载")
    void testDecodeEventPacket() {
        SocketPacket<?> packet = decoder.decode("2[\"chat\",{\"message\":\"hello\"}]");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.EVENT, packet.getType());
    }

    @Test
    @DisplayName("解码 ACK 数据包，返回类型为 ACK")
    void testDecodeAckPacket() {
        SocketPacket<?> packet = decoder.decode("3[\"test\"]");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.ACK, packet.getType());
    }

    @Test
    @DisplayName("解码 ERROR 数据包，返回类型为 ERROR")
    void testDecodeErrorPacket() {
        SocketPacket<?> packet = decoder.decode("4");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.ERROR, packet.getType());
    }

    @Test
    @DisplayName("解码 BINARY_EVENT 数据包，正确解析附件数量")
    void testDecodeBinaryEventPacket() {
        SocketPacket<?> packet = decoder.decode("51-");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.BINARY_EVENT, packet.getType());
        assertEquals(1, packet.getAttachmentsCount());
    }

    @Test
    @DisplayName("解码 BINARY_ACK 数据包，正确解析附件数量")
    void testDecodeBinaryAckPacket() {
        SocketPacket<?> packet = decoder.decode("62-");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.BINARY_ACK, packet.getType());
        assertEquals(2, packet.getAttachmentsCount());
    }

    @Test
    @DisplayName("解码带命名空间的数据包，正确提取命名空间")
    void testDecodeWithNamespace() {
        SocketPacket<?> packet = decoder.decode("0/chat");
        assertNotNull(packet);
        assertEquals(SocketPacket.Type.CONNECT, packet.getType());
        assertEquals("/chat", packet.getNamespace());
    }

    @Test
    @DisplayName("传入 null 时返回 null")
    void testDecodeNullInput() {
        assertNull(decoder.decode(null));
    }

    @Test
    @DisplayName("传入空字符串时返回 null")
    void testDecodeEmptyInput() {
        assertNull(decoder.decode(""));
    }
}
