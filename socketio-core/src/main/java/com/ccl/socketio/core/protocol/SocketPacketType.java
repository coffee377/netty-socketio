package com.ccl.socketio.core.protocol;

public enum SocketPacketType {
    CONNECT(0),
    DISCONNECT(1),
    EVENT(2),
    ACK(3),
    ERROR(4),
    BINARY_EVENT(5),
    BINARY_ACK(6);

    private final int value;

    SocketPacketType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SocketPacketType fromValue(int value) {
        for (SocketPacketType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown Socket.IO packet type: " + value);
    }
}
