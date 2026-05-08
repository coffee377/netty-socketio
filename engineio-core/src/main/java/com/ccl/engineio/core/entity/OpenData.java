package com.ccl.engineio.core.entity;

import com.ccl.engineio.core.protocol.TransportType;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Engine.IO 握手响应数据封装
 *
 * <p>在握手阶段通过 OPEN 数据包发送给客户端，
 * 包含会话 ID、可升级传输方式及心跳配置等信息</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class OpenData {

    /**
     * 会话唯一标识符
     */
    private final String sid;

    /**
     * 可升级的传输方式集合
     */
    private final Set<String> upgrades;

    /**
     * 心跳发送间隔（毫秒）
     */
    private final Integer pingInterval;

    /**
     * 心跳响应超时时间（毫秒）
     */
    private final Integer pingTimeout;

    /**
     * 单个数据包最大负载大小（字节）
     */
    private final Integer maxPayload;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (sid != null) {
            sb.append("{\"sid\":\"").append(sid).append('"');
        }
        sb.append(",\"upgrades\":");
        if (upgrades == null || upgrades.isEmpty()) {
            sb.append("[]");
        } else {
            sb.append('[');
            boolean first = true;
            for (String u : upgrades) {
                if (first) first = false;
                else sb.append(',');
                sb.append('"').append(u).append('"');
            }
            sb.append(']');
        }
        if (pingInterval != null) {
            sb.append(",\"pingInterval\":").append(pingInterval);
        }
        if (pingTimeout != null) {
            sb.append(",\"pingTimeout\":").append(pingTimeout);
        }
        if (maxPayload != null) {
            sb.append(",\"maxPayload\":").append(maxPayload);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * 使用构建器初始化握手数据
     *
     * @param builder 构建器实例
     */
    public OpenData(Builder builder) {
        this.sid = builder.sid;
        this.upgrades = builder.upgrades;
        this.pingInterval = builder.pingInterval;
        this.pingTimeout = builder.pingTimeout;
        this.maxPayload = builder.maxPayload;
    }

    /**
     * 获取会话 ID
     *
     * @return 会话唯一标识符
     */
    public String getSid() {
        return sid;
    }

    /**
     * 获取可升级的传输方式集合
     *
     * @return 传输方式名称集合，如 ["websocket"]
     */
    public Set<String> getUpgrades() {
        return upgrades;
    }

    /**
     * 获取心跳响应超时时间
     *
     * @return 超时时间（毫秒）
     */
    public Integer getPingTimeout() {
        return pingTimeout;
    }

    /**
     * 获取心跳发送间隔
     *
     * @return 间隔时长（毫秒）
     */
    public Integer getPingInterval() {
        return pingInterval;
    }

    /**
     * 获取单个数据包最大负载大小
     *
     * @return 最大字节数
     */
    public Integer getMaxPayload() {
        return maxPayload;
    }

    /**
     * 创建握手数据构建器
     *
     * @param sid 会话唯一标识符
     * @return 新的 Builder 实例
     */
    public static Builder builder(String sid) {
        return new Builder(sid);
    }

    /**
     * 握手数据构建器
     *
     * <p>使用建造者模式创建 {@link OpenData} 实例，
     * 未显式设置的配置项将使用默认值</p>
     */
    public static class Builder {
        private final String sid;
        private Set<String> upgrades;
        private Integer pingInterval;
        private Integer pingTimeout;
        private Integer maxPayload;

        /**
         * 初始化构建器
         *
         * @param sid 会话唯一标识符
         */
        public Builder(String sid) {
            this.sid = sid;
        }

        /**
         * 设置可升级的传输方式
         *
         * <p>过滤空值和空字符串后添加到集合中</p>
         *
         * @param upgrades 传输方式名称数组
         * @return 当前构建器实例
         */
        public Builder upgrades(String... upgrades) {
            if (upgrades == null) {
                this.upgrades = new HashSet<>();
            }
            if (upgrades != null && upgrades.length > 0) {
                List<String> collect = Stream.of(upgrades).filter(Objects::nonNull)
                        .filter(s -> !s.isEmpty()).collect(Collectors.toList());
                this.upgrades.addAll(collect);
            }
            return this;
        }

        /**
         * 添加可升级的传输方式
         *
         * @param transportType 传输类型枚举
         * @return 当前构建器实例
         */
        public Builder upgrade(TransportType transportType) {
            return upgrades(transportType.getName());
        }

        /**
         * 设置心跳发送间隔
         *
         * @param duration 时间间隔
         * @return 当前构建器实例
         */
        public Builder pingInterval(Duration duration) {
            this.pingInterval = Math.toIntExact(duration.toMillis());
            return this;
        }

        /**
         * 设置心跳响应超时时间
         *
         * @param duration 超时时长
         * @return 当前构建器实例
         */
        public Builder pingTimeout(Duration duration) {
            this.pingTimeout = Math.toIntExact(duration.toMillis());
            return this;
        }

        /**
         * 设置单个数据包最大负载大小
         *
         * @param maxPayload 最大字节数
         * @return 当前构建器实例
         */
        public Builder maxPayload(Integer maxPayload) {
            this.maxPayload = maxPayload;
            return this;
        }

        /**
         * 构建握手数据实例
         *
         * <p>未设置的配置项将使用以下默认值：
         * <ul>
         *   <li>pingInterval: 25000 毫秒</li>
         *   <li>pingTimeout: 20000 毫秒</li>
         *   <li>maxPayload: 1000000 字节</li>
         *   <li>upgrades: 空集合</li>
         * </ul>
         * </p>
         *
         * @return 完整的 OpenData 实例
         */
        public OpenData build() {
            if (pingInterval == null) {
                pingInterval = 25_000;
            }
            if (pingTimeout == null) {
                pingTimeout = 20_000;
            }
            if (maxPayload == null) {
                maxPayload = 1_000_000;
            }
            if (upgrades == null) {
                upgrades = new HashSet<>();
            }
            return new OpenData(this);
        }
    }
}
