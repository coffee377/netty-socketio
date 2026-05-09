package com.ccl.socketio.core.codec.impl;

import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.protocol.SocketPacket;

/**
 * Socket.IO V5 协议解码器实现
 *
 * <p>实现 {@link SocketDecoder} 接口，将 Socket.IO 协议的字符串格式解析为 {@link SocketPacket}。
 * 支持的数据包格式：
 * <pre>[type][attachments][namespace][ackId][data]</pre>
 * </p>
 *
 * <p>解析规则：
 * <ul>
 *   <li>首字符：数据包类型（0-6）</li>
 *   <li>BINARY_EVENT/BINARY_ACK：接下来为附件数 + '-'</li>
 *   <li>namespace：以 '/' 开头，以 ',' 分隔</li>
 *   <li>ackId：数字序列</li>
 *   <li>data：JSON 格式负载（当前版本未解析）</li>
 * </ul>
 * </p>
 *
 * @see SocketDecoder
 * @see SocketIOEncoder
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class SocketIODecoder implements SocketDecoder {

    /**
     * 解码 Socket.IO 数据包
     *
     * @param raw 原始字符串数据
     * @return 解析后的数据包实例，raw 为空时返回 null
     * @throws IllegalArgumentException 当数据包格式无效时
     */
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

        }

        return builder.build();
    }
}
