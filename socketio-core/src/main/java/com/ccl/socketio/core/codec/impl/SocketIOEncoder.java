package com.ccl.socketio.core.codec.impl;

import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.ccl.socketio.core.protocol.SocketPacket.Type.BINARY_ACK;
import static com.ccl.socketio.core.protocol.SocketPacket.Type.BINARY_EVENT;

public class SocketIOEncoder implements SocketEncoder {

    private final ObjectMapper mapper = new ObjectMapper();

    public String encode(SocketPacket<?> packet) {
        StringBuilder sb = new StringBuilder();
        sb.append(packet.getType().getValue());

        if (BINARY_EVENT.equals(packet.getType()) || BINARY_ACK.equals(packet.getType())) {
            sb.append(packet.getAttachmentsCount());
            sb.append("-");
        }

        String nsp = packet.getNamespace();
        if (nsp != null && !nsp.isEmpty() && !"/".equals(nsp)) {
            sb.append(nsp);
            sb.append(",");
        }

        if (packet.getAckId() != null) {
            sb.append(packet.getAckId());
        }

        if (packet.getData() != null) {
            // TODO: 2026/05/08 17:28 json 序列化
            try {
                String s = mapper.writeValueAsString(packet.getData());
                sb.append(s);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return sb.toString();
    }

}
