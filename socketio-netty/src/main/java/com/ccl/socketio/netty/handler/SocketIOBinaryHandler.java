package com.ccl.socketio.netty.handler;

import com.ccl.socketio.core.binary.BinaryAttachment;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SocketIOBinaryHandler extends ByteToMessageDecoder {

    private final ConcurrentHashMap<String, BinaryAttachment> pendingAttachments = new ConcurrentHashMap<>();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() > 0) {
            if (in.getByte(in.readerIndex()) == 0) {
                break;
            }
            byte[] data = new byte[in.readableBytes()];
            in.readBytes(data);
            String text = new String(data);
            out.add(text);
        }
    }

    public void registerAttachment(String sessionId, BinaryAttachment attachment) {
        pendingAttachments.put(sessionId, attachment);
    }

    public BinaryAttachment getAttachment(String sessionId) {
        return pendingAttachments.get(sessionId);
    }

    public void removeAttachment(String sessionId) {
        pendingAttachments.remove(sessionId);
    }

    public void addBinaryData(String sessionId, byte[] data) {
        BinaryAttachment attachment = pendingAttachments.get(sessionId);
        if (attachment != null) {
            attachment.addAttachment(data);
            if (attachment.isComplete()) {
                pendingAttachments.remove(sessionId);
            }
        }
    }
}
