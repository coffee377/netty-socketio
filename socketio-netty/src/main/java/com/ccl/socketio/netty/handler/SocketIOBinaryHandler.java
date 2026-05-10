package com.ccl.socketio.netty.handler;

import com.ccl.socketio.core.binary.BinaryAttachment;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket.IO 二进制数据处理处理器
 *
 * <p>负责解码入站的二进制数据流，支持通过 0x00 分隔符分割多个数据段。
 * 同时管理待处理的二进制附件，支持按会话 ID 跟踪附件状态。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
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

    /**
     * 注册待处理的二进制附件
     *
     * @param sessionId  会话 ID
     * @param attachment 二进制附件管理器
     */
    public void registerAttachment(String sessionId, BinaryAttachment attachment) {
        pendingAttachments.put(sessionId, attachment);
    }

    /**
     * 获取指定会话的待处理附件
     *
     * @param sessionId 会话 ID
     * @return 二进制附件管理器，不存在时返回 null
     */
    public BinaryAttachment getAttachment(String sessionId) {
        return pendingAttachments.get(sessionId);
    }

    /**
     * 移除指定会话的待处理附件
     *
     * @param sessionId 会话 ID
     */
    public void removeAttachment(String sessionId) {
        pendingAttachments.remove(sessionId);
    }

    /**
     * 添加二进制数据到指定会话的附件中
     *
     * @param sessionId 会话 ID
     * @param data      二进制数据
     */
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
