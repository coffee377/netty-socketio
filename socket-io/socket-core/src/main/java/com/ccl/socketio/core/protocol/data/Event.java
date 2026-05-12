package com.ccl.socketio.core.protocol.data;

import java.util.List;

/**
 * Socket.IO 事件数据封装
 *
 * <p>用于封装 Socket.IO 事件消息的事件名称和参数列表</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class Event {

    /**
     * 事件名称
     */
    private String name;

    /**
     * 事件参数列表
     */
    private List<Object> args;

    /**
     * 默认构造函数
     */
    public Event() {
    }

    /**
     * 构造事件数据实例
     *
     * @param name 事件名称
     * @param args 事件参数列表
     */
    public Event(String name, List<Object> args) {
        this.name = name;
        this.args = args;
    }

    /**
     * 获取事件名称
     *
     * @return 事件名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置事件名称
     *
     * @param name 事件名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取事件参数列表
     *
     * @return 参数列表
     */
    public List<Object> getArgs() {
        return args;
    }

    /**
     * 设置事件参数列表
     *
     * @param args 参数列表
     */
    public void setArgs(List<Object> args) {
        this.args = args;
    }

}
