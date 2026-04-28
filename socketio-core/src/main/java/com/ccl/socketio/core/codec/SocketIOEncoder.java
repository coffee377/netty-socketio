package com.ccl.socketio.core.codec;

import com.ccl.engineio.util.JsonUtil;
import com.ccl.socketio.core.protocol.SocketPacketType;
import com.ccl.socketio.core.protocol.SocketPacket;

import java.util.List;

public class SocketIOEncoder {

    public String encode(SocketPacket packet) {
        if (packet == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(packet.getType().getValue());

        String namespace = packet.getNamespace();
        if (namespace != null && !namespace.isEmpty()) {
            sb.append(namespace);
            if (!namespace.endsWith(",")) {
                sb.append(",");
            }
        }

        if (packet.getType() == SocketPacketType.EVENT || packet.getType() == SocketPacketType.BINARY_EVENT) {
            if (packet.getEventName() != null) {
                sb.append("\"").append(packet.getEventName()).append("\"");

                List<Object> data = packet.getData();
                if (data != null && !data.isEmpty()) {
                    if (data.size() == 1) {
                        sb.append(JsonUtil.toJson(data.get(0)));
                    } else {
                        sb.append(JsonUtil.toJson(data));
                    }
                }
            }
        } else if (packet.getType() == SocketPacketType.ACK || packet.getType() == SocketPacketType.BINARY_ACK) {
            sb.append(packet.getAckId());
            List<Object> data = packet.getData();
            if (data != null && !data.isEmpty()) {
                sb.append("+");
                if (data.size() == 1) {
                    sb.append(JsonUtil.toJson(data.get(0)));
                } else {
                    sb.append(JsonUtil.toJson(data));
                }
            }
        }

        return sb.toString();
    }

    public String encodeMultiple(SocketPacket... packets) {
        if (packets == null || packets.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (SocketPacket packet : packets) {
            String encoded = encode(packet);
            sb.append(encoded.length()).append("-").append(encoded);
        }
        return sb.toString();
    }

    public byte[] encodeBinary(SocketPacket packet) {
        if (packet == null) {
            return new byte[0];
        }
        return encode(packet).getBytes();
    }
}
