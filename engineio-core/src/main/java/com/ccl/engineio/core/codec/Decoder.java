package com.ccl.socketio.core.codec;

import com.ccl.socketio.core.protocol.SocketPacketType;
import com.ccl.socketio.core.protocol.SocketPacket;

import java.util.ArrayList;
import java.util.List;

public class SocketIODecoder {

    public SocketPacket decode(String rawPacket) {
        if (rawPacket == null || rawPacket.isEmpty()) {
            return null;
        }

        int typeValue = Character.getNumericValue(rawPacket.charAt(0));
        SocketPacketType type = SocketPacketType.fromValue(typeValue);

        SocketPacket packet = new SocketPacket(type);

        int namespaceStart = 1;
        int namespaceEnd = rawPacket.indexOf(',', namespaceStart);
        if (namespaceEnd == -1 && !isNumericOnly(rawPacket.substring(namespaceStart))) {
            namespaceEnd = rawPacket.length();
        }

        if (namespaceEnd > namespaceStart) {
            String namespace = rawPacket.substring(namespaceStart, namespaceEnd);
            packet.setNamespace(namespace);
        } else if (rawPacket.length() > namespaceStart) {
            packet.setNamespace(rawPacket.substring(namespaceStart));
        }

        return packet;
    }

    public List<SocketPacket> decodeAll(String rawPackets) {
        List<SocketPacket> packets = new ArrayList<>();
        if (rawPackets == null || rawPackets.isEmpty()) {
            return packets;
        }

        int i = 0;
        while (i < rawPackets.length()) {
            if (rawPackets.charAt(i) == '-') {
                int dashIndex = rawPackets.indexOf('-', i + 1);
                if (dashIndex == -1) {
                    String single = rawPackets.substring(i + 1);
                    packets.add(decode(single));
                    break;
                }
                String lengthStr = rawPackets.substring(i + 1, dashIndex);
                int length = Integer.parseInt(lengthStr);
                int start = dashIndex + 1;
                int end = Math.min(start + length, rawPackets.length());
                String packet = rawPackets.substring(start, end);
                packets.add(decode(packet));
                i = end;
            } else {
                i++;
            }
        }
        return packets;
    }

    private boolean isNumericOnly(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
