package com.ccl.engineio.parser;

import com.ccl.engineio.core.parser.Parser;
import com.ccl.engineio.core.parser.ParserFactory;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.DataType;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test suite for Engine.IO Parser based on protocol specification
 *
 * @see <a href="https://socket.io/zh-CN/docs/v4/engine-io-protocol/">Engine.IO Protocol</a>
 */
@DisplayName("Engine.IO ParserV4 Tests")
public class ParserV4Test {

    private static final ParserV4 parser = ParserV4.getInstance();

    @Nested
    @DisplayName("Packet Type Tests")
    class PacketTypeTests {

        @Test
        @DisplayName("OPEN packet type should be 0")
        void testOpenPacketType() {
            assertEquals(0, EngineIOPacket.Type.OPEN.getCode());
        }

        @Test
        @DisplayName("CLOSE packet type should be 1")
        void testClosePacketType() {
            assertEquals(1, EngineIOPacket.Type.CLOSE.getCode());
        }

        @Test
        @DisplayName("PING packet type should be 2")
        void testPingPacketType() {
            assertEquals(2, EngineIOPacket.Type.PING.getCode());
        }

        @Test
        @DisplayName("PONG packet type should be 3")
        void testPongPacketType() {
            assertEquals(3, EngineIOPacket.Type.PONG.getCode());
        }

        @Test
        @DisplayName("MESSAGE packet type should be 4")
        void testMessagePacketType() {
            assertEquals(4, EngineIOPacket.Type.MESSAGE.getCode());
        }

        @Test
        @DisplayName("UPGRADE packet type should be 5")
        void testUpgradePacketType() {
            assertEquals(5, EngineIOPacket.Type.UPGRADE.getCode());
        }

        @Test
        @DisplayName("NOOP packet type should be 6")
        void testNoopPacketType() {
            assertEquals(6, EngineIOPacket.Type.NOOP.getCode());
        }
    }

    @Nested
    @DisplayName("Encode Packet Tests")
    class EncodePacketTests {

        @Test
        @DisplayName("Should encode MESSAGE packet with string payload")
        void testEncodeMessagePacketWithString() {
            EngineIOPacket<String> packet = EngineIOPacket.builder().data("hello").build();
            byte[] encoded = parser.encodePacket(packet, true);

            assertEquals('4', (char) encoded[0]);
            assertEquals("hello", new String(encoded, 1, encoded.length - 1, StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Should encode PING packet without payload")
        void testEncodePingPacket() {
            EngineIOPacket<?> packet = EngineIOPacket.builder().type(EngineIOPacket.Type.PING).build();
            byte[] encoded = parser.encodePacket(packet, true);

            assertEquals(1, encoded.length);
            assertEquals('2', (char) encoded[0]);
        }

        @Test
        @DisplayName("Should encode PONG packet with probe payload")
        void testEncodePongPacketWithProbe() {
            EngineIOPacket<String> packet = EngineIOPacket.builder().type(EngineIOPacket.Type.PONG).data("probe").build();
            byte[] encoded = parser.encodePacket(packet, true);

            assertEquals('3', (char) encoded[0]);
            assertEquals("probe", new String(encoded, 1, encoded.length - 1, StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Should encode UPGRADE packet")
        void testEncodeUpgradePacket() {
            EngineIOPacket<?> packet = EngineIOPacket.builder().type(EngineIOPacket.Type.UPGRADE).build();
            byte[] encoded = parser.encodePacket(packet, true);

            assertEquals('5', (char) encoded[0]);
        }

        @Test
        @DisplayName("Should encode binary data when supportsBinary is true")
        void testEncodeBinaryData() {
            byte[] binaryData = new byte[]{0x01, 0x02, 0x03, 0x04};
            EngineIOPacket<byte[]> packet = EngineIOPacket.builder().data(binaryData).build();
            byte[] encoded = parser.encodePacket(packet, true);

            assertEquals('4', (char) encoded[0]);
            byte[] payload = java.util.Arrays.copyOfRange(encoded, 1, encoded.length);
            assertArrayEquals(binaryData, payload);
        }

        @Test
        @DisplayName("Should base64 encode binary data when supportsBinary is false")
        void testEncodeBinaryDataBase64() {
            byte[] binaryData = new byte[]{0x01, 0x02, 0x03, 0x04};
            String expectedBase64 = "AQIDBA==";
            EngineIOPacket<byte[]> packet = EngineIOPacket.builder().data(binaryData).build();
            byte[] encoded = parser.encodePacket(packet, false);

            assertEquals('b', (char) encoded[0]);
            String actualBase64 = new String(encoded, 1, encoded.length - 1, StandardCharsets.UTF_8);
            assertEquals(expectedBase64, actualBase64);
        }
    }

    @Nested
    @DisplayName("Decode Packet Tests")
    class DecodePacketTests {

        @Test
        @DisplayName("Should decode MESSAGE packet with string payload")
        void testDecodeMessagePacket() {
            byte[] encoded = "4hello".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertNotNull(packet);
            assertEquals(EngineIOPacket.Type.MESSAGE, packet.getType());
            assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) packet.getData());
        }

        @Test
        @DisplayName("Should decode PING packet")
        void testDecodePingPacket() {
            byte[] encoded = "2probe".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertNotNull(packet);
            assertEquals(EngineIOPacket.Type.PING, packet.getType());
            assertArrayEquals("probe".getBytes(StandardCharsets.UTF_8), (byte[]) packet.getData());
        }

        @Test
        @DisplayName("Should decode PONG packet")
        void testDecodePongPacket() {
            byte[] encoded = "3probe".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertNotNull(packet);
            assertEquals(EngineIOPacket.Type.PONG, packet.getType());
            assertArrayEquals("probe".getBytes(StandardCharsets.UTF_8), (byte[]) packet.getData());
        }

        @Test
        @DisplayName("Should decode UPGRADE packet")
        void testDecodeUpgradePacket() {
            byte[] encoded = "5".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertNotNull(packet);
            assertEquals(EngineIOPacket.Type.UPGRADE, packet.getType());
            assertNull(packet.getData());
        }

        @Test
        @DisplayName("Should decode NOOP packet")
        void testDecodeNoopPacket() {
            byte[] encoded = "6".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertNotNull(packet);
            assertEquals(EngineIOPacket.Type.NOOP, packet.getType());
            assertNull(packet.getData());
        }

        @Test
        @DisplayName("Should decode base64 encoded binary data")
        void testDecodeBase64BinaryData() {
            byte[] encoded = "bAQIDBA==".getBytes(StandardCharsets.UTF_8);
            byte[] expectedBinary = new byte[]{0x01, 0x02, 0x03, 0x04};
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.BINARY);

            assertNotNull(packet);
            assertEquals(EngineIOPacket.Type.MESSAGE, packet.getType());
            assertArrayEquals(expectedBinary, (byte[]) packet.getData());
        }

        @Test
        @DisplayName("Should handle null data")
        void testDecodeNullData() {
            EngineIOPacket<?> packet = parser.decodePacket(null, DataType.PLAINTEXT);
            assertNull(packet);
        }

        @Test
        @DisplayName("Should handle empty data")
        void testDecodeEmptyData() {
            byte[] encoded = new byte[0];
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);
            assertNotNull(packet);
            assertEquals(EngineIOPacket.Type.MESSAGE, packet.getType());
        }

        @Test
        @DisplayName("Should decode string input")
        void testDecodeStringInput() {
            EngineIOPacket<?> packet = parser.decodePacket("4hello", DataType.PLAINTEXT);

            assertNotNull(packet);
            assertEquals(EngineIOPacket.Type.MESSAGE, packet.getType());
            assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) packet.getData());
        }
    }

    @Nested
    @DisplayName("Payload Encoding Tests (HTTP Long-Polling)")
    class PayloadEncodingTests {

        @Test
        @DisplayName("Should encode multiple packets in payload")
        void testEncodePayloadMultiplePackets() {
            List<EngineIOPacket<?>> packets = Arrays.asList(
                    EngineIOPacket.builder().data("hello").build(),
                    EngineIOPacket.builder().type(EngineIOPacket.Type.PING).build(),
                    EngineIOPacket.builder().data("world").build()
            );

            ByteBuffer buffer = parser.encodePayload(packets, true);
            byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);

            String result = new String(payload, StandardCharsets.UTF_8);
            assertTrue(result.startsWith("4hello"));
            assertTrue(result.contains("\u001E"));
            assertTrue(result.contains("2"));
            assertTrue(result.endsWith("4world"));
        }

        @Test
        @DisplayName("Should decode multiple packets from payload")
        void testDecodePayloadMultiplePackets() {
            // 4hello\x1e2\x1e4world
            String encoded = "4hello\u001E2\u001E4world";
            byte[] payload = encoded.getBytes(StandardCharsets.UTF_8);

            List<EngineIOPacket<?>> packets = parser.decodePayload(payload, DataType.PLAINTEXT);

            assertEquals(3, packets.size());
            assertEquals(EngineIOPacket.Type.MESSAGE, packets.get(0).getType());
            assertEquals(EngineIOPacket.Type.PING, packets.get(1).getType());
            assertEquals(EngineIOPacket.Type.MESSAGE, packets.get(2).getType());
        }

        @Test
        @DisplayName("Should encode payload with base64 binary data")
        void testEncodePayloadWithBinary() {
            List<EngineIOPacket<?>> packets = Arrays.asList(
                    EngineIOPacket.builder().data("hello").build(),
                    EngineIOPacket.builder().data(new byte[]{0x01, 0x02, 0x03, 0x04}).build()
            );

            ByteBuffer buffer = parser.encodePayload(packets, false);
            byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);

            String result = new String(payload, StandardCharsets.UTF_8);
            assertTrue(result.startsWith("4hello"));
            assertTrue(result.contains("bAQIDBA=="));
        }

        @Test
        @DisplayName("Should decode payload with base64 binary data")
        void testDecodePayloadWithBinary() {
            // 4hello\x1ebAQIDBA==
            String encoded = "4hello\u001EbAQIDBA==";
            byte[] payload = encoded.getBytes(StandardCharsets.UTF_8);

            List<EngineIOPacket<?>> packets = parser.decodePayload(payload, DataType.BINARY);

            assertEquals(2, packets.size());
            assertEquals(EngineIOPacket.Type.MESSAGE, packets.get(0).getType());
            assertEquals(EngineIOPacket.Type.MESSAGE, packets.get(1).getType());
        }

        @Test
        @DisplayName("Should handle empty payload")
        void testEncodeDecodeEmptyPayload() {
            List<EngineIOPacket<?>> packets = Collections.emptyList();

            java.nio.ByteBuffer buffer = parser.encodePayload(packets, true);
            assertEquals(0, buffer.remaining());

            List<EngineIOPacket<?>> decoded = parser.decodePayload(new byte[0], DataType.PLAINTEXT);
            assertTrue(decoded.isEmpty());
        }
    }

    @Nested
    @DisplayName("Protocol Examples from Documentation")
    class ProtocolExamplesTests {

        @Test
        @DisplayName("WebSocket frame: 4hello (message packet)")
        void testWebSocketMessageExample() {
            byte[] encoded = "4hello".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertEquals(EngineIOPacket.Type.MESSAGE, packet.getType());
            assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) packet.getData());
        }

        @Test
        @DisplayName("WebSocket frame: 2probe (ping with probe)")
        void testWebSocketPingExample() {
            byte[] encoded = "2probe".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertEquals(EngineIOPacket.Type.PING, packet.getType());
            assertArrayEquals("probe".getBytes(StandardCharsets.UTF_8), (byte[]) packet.getData());
        }

        @Test
        @DisplayName("WebSocket frame: 3probe (pong with probe)")
        void testWebSocketPongExample() {
            byte[] encoded = "3probe".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertEquals(EngineIOPacket.Type.PONG, packet.getType());
            assertArrayEquals("probe".getBytes(StandardCharsets.UTF_8), (byte[]) packet.getData());
        }

        @Test
        @DisplayName("WebSocket frame: 5 (upgrade packet)")
        void testWebSocketUpgradeExample() {
            byte[] encoded = "5".getBytes(StandardCharsets.UTF_8);
            EngineIOPacket<?> packet = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertEquals(EngineIOPacket.Type.UPGRADE, packet.getType());
            assertNull(packet.getData());
        }

        @Test
        @DisplayName("Payload: 4hello\\x1e2\\x1e4world")
        void testPayloadExample() {
            String payload = "4hello\u001E2\u001E4world";
            List<EngineIOPacket<?>> packets = parser.decodePayload(payload, DataType.PLAINTEXT);

            assertEquals(3, packets.size());

            assertEquals(EngineIOPacket.Type.MESSAGE, packets.get(0).getType());
            assertEquals("hello", new String((byte[]) packets.get(0).getData(), StandardCharsets.UTF_8));

            assertEquals(EngineIOPacket.Type.PING, packets.get(1).getType());
            assertNull(packets.get(1).getData());

            assertEquals(EngineIOPacket.Type.MESSAGE, packets.get(2).getType());
            assertEquals("world", new String((byte[]) packets.get(2).getData(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Payload with binary: 4hello\\x1ebAQIDBA==")
        void testPayloadWithBinaryExample() {
            String payload = "4hello\u001EbAQIDBA==";
            List<EngineIOPacket<?>> packets = parser.decodePayload(payload, DataType.BINARY);

            assertEquals(2, packets.size());

            assertEquals(EngineIOPacket.Type.MESSAGE, packets.get(0).getType());
            assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) packets.get(0).getData());

            assertEquals(EngineIOPacket.Type.MESSAGE, packets.get(1).getType());
            assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, (byte[]) packets.get(1).getData());
        }

        @Test
        @DisplayName("Round-trip: encode then decode packet")
        void testEncodeDecodeRoundTrip() {
            String message = "test message";
            EngineIOPacket<String> original = EngineIOPacket.builder().data(message).build();
            byte[] encoded = parser.encodePacket(original, true);
            EngineIOPacket<?> decoded = parser.decodePacket(encoded, DataType.PLAINTEXT);

            assertEquals(original.getType(), decoded.getType());
            assertEquals(message, new String((byte[]) decoded.getData(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Round-trip: encode then decode binary packet")
        void testEncodeDecodeBinaryRoundTrip() {
            byte[] binaryData = new byte[]{0x01, 0x02, 0x03, 0x04};
            EngineIOPacket<byte[]> original = EngineIOPacket.builder().data(binaryData).build();
            byte[] encoded = parser.encodePacket(original, true);
            EngineIOPacket<?> decoded = parser.decodePacket(encoded, DataType.BINARY);

            assertEquals(original.getType(), decoded.getType());
            assertArrayEquals(binaryData, (byte[]) decoded.getData());
        }
    }

    @Nested
    @DisplayName("ParserFactory Tests")
    class ParserFactoryTests {

        @Test
        @DisplayName("Should get default parser")
        void testGetDefaultParser() {
            Parser parser = ParserFactory.getDefaultParser();
            assertNotNull(parser);
            assertEquals(4, parser.getProtocolVersion());
        }

        @Test
        @DisplayName("Should get parser by version V4")
        void testGetParserByV4() {
            Parser parser = ParserFactory.getParser("4");
            assertNotNull(parser);
            assertEquals(4, parser.getProtocolVersion());
        }

        @Test
        @DisplayName("Should get parser by version V3")
        void testGetParserByV3() {
            Parser parser = ParserFactory.getParser("3");
            assertNotNull(parser);
            assertEquals(4, parser.getProtocolVersion());
        }

        @Test
        @DisplayName("Should get parser for unknown version")
        void testGetParserUnknownVersion() {
            Parser parser = ParserFactory.getParser("unknown");
            assertNotNull(parser);
            assertEquals(4, parser.getProtocolVersion());
        }
    }

    @Nested
    @DisplayName("EngineIOPacket Tests")
    class EngineIOPacketTests {

        @Test
        @DisplayName("Should create packet with type and data")
        void testOfWithTypeAndData() {
            EngineIOPacket<byte[]> packet = EngineIOPacket.builder().data("test".getBytes()).build();
            assertEquals(EngineIOPacket.Type.MESSAGE, packet.getType());
            assertArrayEquals("test".getBytes(), packet.getData());
        }

        @Test
        @DisplayName("Should create packet with type only")
        void testOfWithTypeOnly() {
            EngineIOPacket<?> packet = EngineIOPacket.builder().type(EngineIOPacket.Type.PING).build();
            assertEquals(EngineIOPacket.Type.PING, packet.getType());
            assertNull(packet.getData());
        }

        @Test
        @DisplayName("Should create MESSAGE packet from data")
        void testOfWithDataOnly() {
            EngineIOPacket<String> packet = EngineIOPacket.builder().data("hello").build();
            assertEquals(EngineIOPacket.Type.MESSAGE, packet.getType());
            assertEquals("hello", packet.getData());
        }

        @Test
        @DisplayName("Should convert type to byte")
        void testTypeToByte() {
            assertEquals((byte) '0', EngineIOPacket.Type.OPEN.getByte());
            assertEquals((byte) '4', EngineIOPacket.Type.MESSAGE.getByte());
        }

        @Test
        @DisplayName("Should convert byte to type")
        void testByteToType() {
            assertEquals(EngineIOPacket.Type.OPEN, EngineIOPacket.Type.fromByte((byte) '0'));
            assertEquals(EngineIOPacket.Type.MESSAGE, EngineIOPacket.Type.fromByte((byte) '4'));
        }
    }

    @Nested
    @DisplayName("DataType Tests")
    class DataTypeTests {

        @Test
        @DisplayName("PLAINTEXT type value should be 0")
        void testPlaintextValue() {
            assertEquals(0, DataType.PLAINTEXT.getValue());
        }

        @Test
        @DisplayName("BINARY type value should be 1")
        void testBinaryValue() {
            assertEquals(1, DataType.BINARY.getValue());
        }

        @Test
        @DisplayName("Should convert to byte")
        void testToByte() {
            assertEquals((byte) 0, DataType.PLAINTEXT.toByte());
            assertEquals((byte) 1, DataType.BINARY.toByte());
        }

        @Test
        @DisplayName("Should convert from byte")
        void testFromByte() {
            assertEquals(DataType.PLAINTEXT, DataType.fromByte((byte) 0));
            assertEquals(DataType.BINARY, DataType.fromByte((byte) 1));
        }
    }
}

