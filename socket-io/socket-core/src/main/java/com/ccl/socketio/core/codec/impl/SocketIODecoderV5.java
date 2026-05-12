package com.ccl.socketio.core.codec.impl;

import com.ccl.io.engine.core.codec.impl.JacksonCodec;
import com.ccl.io.engine.codec.Codec;
import com.ccl.socketio.core.codec.SocketDecoder;
import com.ccl.socketio.core.protocol.SocketPacket;
import com.ccl.socketio.core.protocol.data.Event;

import java.util.*;

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
 * @author coffee377
 * @see SocketDecoder
 * @see SocketIOEncoderV5
 * @since 4.0.0-alpha.0
 */
public class SocketIODecoderV5 implements SocketDecoder {

    /**
     * 字符串编解码器，用于 JSON 解析
     */
    private final Codec stringCodec;

    /**
     * 构造函数
     *
     * @param stringCodec 字符串编解码器
     */
    public SocketIODecoderV5(Codec stringCodec) {
        this.stringCodec = stringCodec;
    }

    /**
     * 默认构造函数
     */
    public SocketIODecoderV5() {
        this(new JacksonCodec());
    }

    /**
     * 判断是否支持指定协议版本
     *
     * @param protocolVersion 协议版本号
     * @return 版本号为 5 时返回 true
     */
    @Override
    public boolean isSupport(int protocolVersion) {
        return protocolVersion == 5;
    }

    /**
     * 获取字符串编解码器
     *
     * @return 字符串编解码器实例
     */
    @Override
    public Codec getStringCodec() {
        return stringCodec;
    }

    /**
     * 解码 Socket.IO 数据包
     *
     * @param raw 原始字符串数据
     * @return 解析后的数据包实例，raw 为空时返回 null
     * @throws IllegalArgumentException 当数据包格式无效时
     */
    public SocketPacket<?> decode(String raw) {
        return decode(raw, Object.class);
    }

    /**
     * 解码 Socket.IO 数据包为指定类型
     *
     * @param raw   原始字符串数据
     * @param clazz 目标数据类型
     * @param <T>   目标类型
     * @return 解析后的数据包实例，raw 为空时返回 null
     * @throws IllegalArgumentException 当数据包格式无效时
     */
    @SuppressWarnings("unchecked")
    public <T> SocketPacket<T> decode(String raw, Class<T> clazz) {
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

        if (length > i + 1) {
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
                    builder.ackId(Long.parseLong(id.toString()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid payload");
                }
            }
        }

        if (length > i + 1) {
            String json = raw.substring(i + 1);
            Codec codec = getStringCodec();
            if (codec != null) {
                T data = codec.deserialize(json, clazz);
                if (data instanceof Event) {
                    Event event = (Event) data;
                    builder.event(event.getName());
                    builder.data(event.getArgs());
                } else if (data instanceof Collection) {
                    Collection<?> collection = (Collection<?>) data;
                    List<?> list = new ArrayList<>(collection);

                    if (!list.isEmpty() && list.get(0) instanceof String) {
                        builder.event((String) list.get(0));
                        list.remove(0);
                    }
                    builder.data(list);
                } else {
                    builder.data(data);
                }
            }
        }

        return (SocketPacket<T>) builder.build();
    }
}
