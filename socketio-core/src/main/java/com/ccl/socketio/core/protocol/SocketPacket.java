package com.ccl.socketio.core.protocol;

import java.util.List;

public class SocketPacket {

    private SocketPacketType type;
    private String namespace;
    private String eventName;
    private List<Object> data;
    private int ackId;
    private byte[][] binaryAttachments;
    private boolean hasBinary;

    public SocketPacket(SocketPacketType type) {
        this(type, "/");
    }

    public SocketPacket(SocketPacketType type, String namespace) {
        this.type = type;
        this.namespace = namespace;
    }

    public SocketPacketType getType() {
        return type;
    }

    public void setType(SocketPacketType type) {
        this.type = type;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public List<Object> getData() {
        return data;
    }

    public void setData(List<Object> data) {
        this.data = data;
    }

    public int getAckId() {
        return ackId;
    }

    public void setAckId(int ackId) {
        this.ackId = ackId;
    }

    public byte[][] getBinaryAttachments() {
        return binaryAttachments;
    }

    public void setBinaryAttachments(byte[][] binaryAttachments) {
        this.binaryAttachments = binaryAttachments;
        this.hasBinary = binaryAttachments != null && binaryAttachments.length > 0;
    }

    public boolean hasBinary() {
        return hasBinary;
    }

    public static SocketPacket connectPacket(String namespace) {
        SocketPacket packet = new SocketPacket(SocketPacketType.CONNECT, namespace);
        return packet;
    }

    public static SocketPacket disconnectPacket(String namespace) {
        SocketPacket packet = new SocketPacket(SocketPacketType.DISCONNECT, namespace);
        return packet;
    }

    public static SocketPacket eventPacket(String namespace, String eventName, List<Object> data) {
        SocketPacket packet = new SocketPacket(SocketPacketType.EVENT, namespace);
        packet.setEventName(eventName);
        packet.setData(data);
        return packet;
    }

    public static SocketPacket ackPacket(String namespace, int ackId, List<Object> data) {
        SocketPacket packet = new SocketPacket(SocketPacketType.ACK, namespace);
        packet.setAckId(ackId);
        packet.setData(data);
        return packet;
    }

    public static SocketPacket binaryEventPacket(String namespace, String eventName, List<Object> data, byte[][] binaries) {
        SocketPacket packet = new SocketPacket(SocketPacketType.BINARY_EVENT, namespace);
        packet.setEventName(eventName);
        packet.setData(data);
        packet.setBinaryAttachments(binaries);
        return packet;
    }

    public static SocketPacket binaryAckPacket(String namespace, int ackId, List<Object> data, byte[][] binaries) {
        SocketPacket packet = new SocketPacket(SocketPacketType.BINARY_ACK, namespace);
        packet.setAckId(ackId);
        packet.setData(data);
        packet.setBinaryAttachments(binaries);
        return packet;
    }
}
