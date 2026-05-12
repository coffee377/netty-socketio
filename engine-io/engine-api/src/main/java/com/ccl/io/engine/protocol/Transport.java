package com.ccl.io.engine.protocol;

import java.util.Arrays;

/**
 * Engine.IO 传输类型枚举
 *
 * @author coffee377
 * @see <a href="https://socket.io/zh-CN/docs/v4/how-it-works/">Socket.IO 工作原理</a>
 */
public enum Transport {

    /**
     * HTTP 长轮询
     */
    POLLING("polling", "HTTP long-polling，WebSocket 不可用时降级方案"),

    /**
     * WebSocket 全双工通信
     */
    WEBSOCKET("websocket", "WebSocket，优先使用的高性能传输");

    /**
     * 传输类型名称（URL 参数值）
     */
    private final String name;

    /**
     * 传输类型描述
     */
    private final String description;

    Transport(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 获取传输类型名称。
     *
     * @return 传输类型名称（用于 URL 参数）
     */
    public String getName() {
        return name;
    }

    /**
     * 获取传输类型描述。
     *
     * @return 传输类型的功能说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据传输类型名称查找对应的枚举值。
     *
     * @param name 传输类型名称（不区分大小写）
     * @return 对应的枚举值，未找到时返回 null
     */
    public static Transport of(String name) {
        return Arrays.stream(values()).filter(t -> t.name.equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

}
