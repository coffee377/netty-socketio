package com.ccl.socketio.core.codec;

import com.ccl.socketio.core.codec.impl.SocketIOEncoderV5;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.core.protocol.data.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SocketIOEncoderV5 编码测试")
class SocketEncoderTest {

    static SocketEncoder socketEncoder;

    @BeforeAll
    static void setup() {
        socketEncoder = new SocketIOEncoderV5();
    }

    @Test
    @DisplayName("序列化 BINARY_EVENT，Event 输出 JSON 数组，byte[] 替换为二进制占位对象")
    void encode() {
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);

        Event event = new Event();
        event.setName("pong");
        event.setArgs(Arrays.asList(bytes, bytes));
        // 预期输出：51-["pong",{"_placeholder":true,"num":0},{"_placeholder":true,"num":1}]
        SocketPacket<?> packet = SocketPacket.builder()
                .type(SocketPacket.Type.BINARY_EVENT)
                .attachmentsCount(1)
                .data(event).build();
        packet.addAttachment(bytes);

        String result = socketEncoder.encode(packet);
        assertEquals("51-[\"pong\",{\"_placeholder\":true,\"num\":0},{\"_placeholder\":true,\"num\":1}]", result);
    }

}
