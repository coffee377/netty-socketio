package com.ccl.socketio.core.ack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Socket.IO ACK 确认管理器
 *
 * <p>管理待处理的 ACK 回调，支持注册、触发、取消和定时清理超时 ACK。
 * 使用单例模式，内部通过 ScheduledExecutorService 定期清理过期条目。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class AckManager {

    private static final AckManager INSTANCE = new AckManager();
    private final Map<String, AckEntry> pendingAcks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private AckManager() {
        scheduler.scheduleAtFixedRate(this::cleanup, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 获取 AckManager 单例实例
     *
     * @return AckManager 实例
     */
    public static AckManager getInstance() {
        return INSTANCE;
    }

    /**
     * 注册一个待处理的 ACK 回调
     *
     * @param ackId    ACK 唯一标识
     * @param callback 回调函数，接收确认数据
     * @param timeout  超时时间（毫秒）
     */
    public void registerAck(String ackId, Consumer<Object> callback, long timeout) {
        pendingAcks.put(ackId, new AckEntry(callback, System.currentTimeMillis() + timeout));
    }

    /**
     * 触发指定 ACK 的回调
     *
     * @param ackId ACK 唯一标识
     * @param data  确认数据
     */
    public void triggerAck(String ackId, Object data) {
        AckEntry entry = pendingAcks.remove(ackId);
        if (entry != null && entry.callback != null) {
            try {
                entry.callback.accept(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 取消指定 ACK
     *
     * @param ackId ACK 唯一标识
     */
    public void cancelAck(String ackId) {
        pendingAcks.remove(ackId);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        pendingAcks.entrySet().removeIf(entry -> entry.getValue().timeout < now);
    }

    /**
     * 关闭管理器，释放调度线程资源
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    /**
     * ACK 条目，包含回调函数和超时时间
     */
    private static class AckEntry {
        final Consumer<Object> callback;
        final long timeout;

        AckEntry(Consumer<Object> callback, long timeout) {
            this.callback = callback;
            this.timeout = timeout;
        }
    }
}