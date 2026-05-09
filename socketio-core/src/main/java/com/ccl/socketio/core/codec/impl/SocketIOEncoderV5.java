package com.ccl.socketio.core.codec.impl;

import com.ccl.socketio.core.codec.SocketEncoder;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.ccl.socketio.core.protocol.SocketPacket.Type.BINARY_ACK;
import static com.ccl.socketio.core.protocol.SocketPacket.Type.BINARY_EVENT;

/**
 * Socket.IO V5 协议编码器实现
 *
 * <p>实现 {@link SocketEncoder} 接口，将 {@link SocketPacket} 编码为 Socket.IO 协议的字符串格式。
 * 编码后格式：
 * <pre>[type][attachments][namespace][ackId][JSON data]</pre>
 * </p>
 *
 * <p>编码规则：
 * <ul>
 *   <li>首字符：数据包类型数字（0-6）</li>
 *   <li>BINARY_EVENT/BINARY_ACK：添加附件数 + '-'</li>
 *   <li>非默认 namespace：追加 namespace + ','</li>
 *   <li>有 ackId：追加数字</li>
 *   <li>有 data：JSON 序列化后追加</li>
 * </ul>
 * </p>
 *
 * @see SocketDecoder
 * @see SocketIODecoder
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class SocketIOEncoder implements SocketEncoder {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 编码 Socket.IO 数据包
     *
     * @param packet 数据包实例
     * @return 编码后的字符串
     */
    public String encode(SocketPacket<?> packet) {
        StringBuilder sb = new StringBuilder();
        sb.append(packet.getType().getValue());

        if (BINARY_EVENT.equals(packet.getType()) || BINARY_ACK.equals(packet.getType())) {
            sb.append(packet.getAttachmentsCount());
            sb.append("-");
        }

        String nsp = packet.getNamespace();
        if (nsp != null && !nsp.isEmpty() && !"/".equals(nsp)) {
            sb.append(nsp);
            sb.append(",");
        }

        if (packet.getAckId() != null) {
            sb.append(packet.getAckId());
        }

        if (packet.getData() != null) {
            try {
                String s = mapper.writeValueAsString(packet.getData());
                sb.append(s);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return sb.toString();
    }

}
