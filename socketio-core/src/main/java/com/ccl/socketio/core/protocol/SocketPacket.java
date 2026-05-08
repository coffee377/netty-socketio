package com.ccl.socketio.core.protocol;

import java.util.ArrayList;
import java.util.List;

import static com.ccl.socketio.core.protocol.SocketPacket.Type.BINARY_ACK;
import static com.ccl.socketio.core.protocol.SocketPacket.Type.BINARY_EVENT;

// <packet type>[<# of binary attachments>-][<namespace>,][<acknowledgment id>][JSON-stringified payload without binary]
//
//+ binary attachments extracted
public class SocketPacket<T> {
    private final Type type;
    private final int attachmentsCount;
    private final String namespace;
    private final Long ackId;
    private String eventName;
    private final T data;
    private final List<byte[]> attachments;

    private String dataSource;

    private SocketPacket(Builder<T> builder) {
        this.type = builder.type;
        this.attachmentsCount = builder.attachmentsCount;
        this.namespace = builder.namespace;
        this.ackId = builder.ackId;
        this.data = builder.data;
        this.attachments = new ArrayList<>();
    }

    public static <D> SocketPacket.Builder<D> builder() {
        return new SocketPacket.Builder<>();
    }

    public Type getType() {
        return type;
    }

    public int getAttachmentsCount() {
        return attachmentsCount;
    }

    public String getNamespace() {
        return namespace;
    }

    public Long getAckId() {
        return ackId;
    }

    public String getEventName() {
        return eventName;
    }

    public T getData() {
        return data;
    }

    public List<byte[]> getAttachments() {
        return attachments;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void addAttachment(byte[] binaryAttachment) {
        if (this.attachments.size() < attachmentsCount) {
            this.attachments.add(binaryAttachment);
        }
    }

    public boolean hasAttachments() {
        return attachmentsCount != 0;
    }

    public boolean isAttachmentsLoaded() {
        return this.attachments.size() == attachmentsCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getType().getValue());

        if (BINARY_EVENT.equals(this.getType()) || BINARY_ACK.equals(this.getType())) {
            sb.append(this.getAttachmentsCount());
            sb.append("-");
        }

        String nsp = this.getNamespace();
        if (nsp != null && !nsp.isEmpty() && !"/".equals(nsp)) {
            sb.append(nsp);
            sb.append(",");
        }

        if (this.getAckId() != null) {
            sb.append(this.getAckId());
        }

        if (this.getData() != null) {
            String string = this.getData().toString();
            if (string != null && !string.isEmpty()) {
                sb.append(string);
            }
        }

        return sb.toString();
    }

    public enum Type {
        CONNECT(0),
        DISCONNECT(1),
        EVENT(2),
        ACK(3),
        ERROR(4),
        BINARY_EVENT(5),
        BINARY_ACK(6);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type fromValue(int value) {
            for (Type type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown Socket.IO packet type: " + value);
        }
    }

    public static class Builder<D> {
        private Type type;
        private int attachmentsCount;
        private String namespace;
        private Long ackId;
        private D data;

        public Builder<D> type(Type type) {
            this.type = type;
            return this;
        }

        public Builder<D> attachmentsCount(int attachmentsCount) {
            this.attachmentsCount = attachmentsCount;
            return this;
        }

        public Builder<D> namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder<D> ackId(Long ackId) {
            this.ackId = ackId;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> Builder<T> data(T data) {
            this.data = (D) data;
            return (Builder<T>) this;
        }

        public SocketPacket<D> build() {
            return new SocketPacket<>(this);
        }
    }

}
