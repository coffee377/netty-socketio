package com.ccl.engineio.core.protocol;

/**
 * Engine.IO 传输类型枚举
 *
 * @author coffee377
 * @see <a href="https://socket.io/zh-CN/docs/v4/how-it-works/">Socket.IO 工作原理</a>
 */
public enum TransportType {

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

    /**
     * 构造函数
     *
     * @param name        传输类型名称
     * @param description 传输类型描述
     */
    TransportType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 获取传输类型名称
     *
     * @return 传输类型名称（用于 URL 参数）
     */
    public String getName() {
        return name;
    }

    /**
     * 获取传输类型描述
     *
     * @return 传输类型的功能说明
     */
    public String getDescription() {
        return description;
    }

}
