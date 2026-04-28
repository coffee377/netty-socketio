package com.ccl.socketio.core.binary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BinaryAttachment {

    private final String placeholder;
    private final List<ByteBuffer> attachments;
    private int currentIndex;

    public BinaryAttachment(int attachmentCount) {
        this.placeholder = createPlaceholder(attachmentCount);
        this.attachments = new ArrayList<>(attachmentCount);
        this.currentIndex = 0;
    }

    public void addAttachment(ByteBuffer buffer) {
        attachments.add(buffer);
    }

    public void addAttachment(byte[] data) {
        attachments.add(ByteBuffer.wrap(data));
    }

    public ByteBuffer getAttachment(int index) {
        if (index >= 0 && index < attachments.size()) {
            return attachments.get(index);
        }
        return null;
    }

    public int getAttachmentCount() {
        return attachments.size();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public List<ByteBuffer> getAllAttachments() {
        return new ArrayList<>(attachments);
    }

    private String createPlaceholder(int count) {
        StringBuilder sb = new StringBuilder("___");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append("_");
            sb.append("BINARY").append(i).append("___");
        }
        return sb.toString();
    }

    public boolean isComplete() {
        return currentIndex >= attachments.size();
    }

    public void reset() {
        currentIndex = 0;
    }

    public static String replacePlaceholders(String data, List<ByteBuffer> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return data;
        }
        String result = data;
        for (int i = 0; i < attachments.size(); i++) {
            String placeholder = "___" + "BINARY" + i + "___";
            result = result.replace(placeholder, "");
        }
        return result;
    }
}