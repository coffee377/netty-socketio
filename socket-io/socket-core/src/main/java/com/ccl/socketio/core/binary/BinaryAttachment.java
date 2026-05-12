package com.ccl.socketio.core.binary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Socket.IO 二进制附件管理
 *
 * <p>管理与 Socket.IO 数据包关联的二进制附件数据，支持添加、获取和占位符替换操作。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class BinaryAttachment {

    private final String placeholder;
    private final List<ByteBuffer> attachments;
    private int currentIndex;

    /**
     * 创建二进制附件管理器
     *
     * @param attachmentCount 预期的附件数量
     */
    public BinaryAttachment(int attachmentCount) {
        this.placeholder = createPlaceholder(attachmentCount);
        this.attachments = new ArrayList<>(attachmentCount);
        this.currentIndex = 0;
    }

    /**
     * 添加二进制附件
     *
     * @param buffer 附件字节缓冲区
     */
    public void addAttachment(ByteBuffer buffer) {
        attachments.add(buffer);
    }

    /**
     * 添加二进制附件
     *
     * @param data 附件字节数组
     */
    public void addAttachment(byte[] data) {
        attachments.add(ByteBuffer.wrap(data));
    }

    /**
     * 获取指定索引的附件
     *
     * @param index 附件索引
     * @return 附件字节缓冲区，索引越界时返回 null
     */
    public ByteBuffer getAttachment(int index) {
        if (index >= 0 && index < attachments.size()) {
            return attachments.get(index);
        }
        return null;
    }

    /**
     * 获取已添加的附件数量
     *
     * @return 附件数量
     */
    public int getAttachmentCount() {
        return attachments.size();
    }

    /**
     * 获取占位符字符串
     *
     * @return 占位符
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * 获取所有附件的副本
     *
     * @return 附件列表副本
     */
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

    /**
     * 判断是否所有附件均已加载完成
     *
     * @return 加载完成时返回 true
     */
    public boolean isComplete() {
        return currentIndex >= attachments.size();
    }

    /**
     * 重置附件索引状态
     */
    public void reset() {
        currentIndex = 0;
    }

    /**
     * 替换字符串中的占位符
     *
     * @param data       原始字符串
     * @param attachments 附件列表
     * @return 移除占位符后的字符串
     */
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