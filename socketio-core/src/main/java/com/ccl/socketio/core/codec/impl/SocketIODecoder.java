package com.ccl.socketio.core.codec.impl;

import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.protocol.SocketPacket;

public class SocketIODecoder implements SocketDecoder {

    public SocketPacket<?> decode(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        int i = 0;
        int length = raw.length();
        int typeValue = Character.getNumericValue(raw.charAt(0));
        SocketPacket.Type type = SocketPacket.Type.fromValue(typeValue);

        SocketPacket.Builder<?> builder = SocketPacket.builder().type(type);

        if (SocketPacket.Type.BINARY_EVENT.equals(type) || SocketPacket.Type.BINARY_ACK.equals(type)) {
            if (!raw.contains("-") || length <= i + 1) {
                throw new IllegalArgumentException("illegal attachments");
            }
            StringBuilder attachments = new StringBuilder();
            while (raw.charAt(++i) != '-') {
                attachments.append(raw.charAt(i));
            }
            builder.attachmentsCount(Integer.parseInt(attachments.toString()));
        }

        if (length > i + 1 && '/' == raw.charAt(i + 1)) {
            StringBuilder nsp = new StringBuilder();
            while (true) {
                ++i;
                char c = raw.charAt(i);
                if (',' == c) break;
                nsp.append(c);
                if (i + 1 == length) break;
            }
            builder.namespace(nsp.toString());
        } else {
            builder.namespace("/");
        }

        if (length > i + 1){
            char next = raw.charAt(i + 1);
            if (Character.getNumericValue(next) > -1) {
                StringBuilder id = new StringBuilder();
                while (true) {
                    ++i;
                    char c = raw.charAt(i);
                    if (Character.getNumericValue(c) < 0) {
                        --i;
                        break;
                    }
                    id.append(c);
                    if (i + 1 == length) break;
                }
                try {
                    long l = Long.parseLong(id.toString());
                    builder.ackId(l);
                } catch (NumberFormatException e){
                    throw new IllegalArgumentException("invalid payload");
                }
            }
        }

        if (length > i + 1){
//            try {
//                raw.charAt(++i);
//                p.data = new JSONTokener(raw.substring(i)).nextValue();
//            } catch (JSONException e) {
//                logger.log(Level.WARNING, "An error occured while retrieving data from JSONTokener", e);
//                throw new DecodingException("invalid payload");
//            }
//            if (!isPayloadValid(p.type, p.data)) {
//                throw new DecodingException("invalid payload");
//            }
        }

        return builder.build();
    }
}
