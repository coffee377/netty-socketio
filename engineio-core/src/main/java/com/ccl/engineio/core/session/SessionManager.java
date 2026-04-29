package com.ccl.engineio.core.session;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.exception.SessionNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Engine.IO 会话管理器（单例）
 *
 * <p>负责客户端会话的创建、查询、销毁和超时管理。
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final ConcurrentHashMap<String, ClientContext> sessions = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ScheduledFuture<?>> sessionTimeoutTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private long pingTimeout = 25000;

    private long pingInterval = 30000;

    private SessionManager() {
    }

    /**
     * 获取单例实例。
     *
     * @return SessionManager 实例
     */
    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * 创建新会话。
     *
     * @return 会话 ID（UUID，去掉连字符）
     * @deprecated Use {@link #createSession(TransportType)} instead
     */
    @Deprecated
    public String createSession() {
        ClientContext session = createSession(TransportType.POLLING);
        return session.getSessionId();
    }

    /**
     * 创建新会话。
     *
     * @param transportType 传输类型（polling 或 websocket）
     * @return 客户端上下文对象
     */
    public ClientContext createSession(TransportType transportType) {
        String sid = UUID.randomUUID().toString().replace("-", "");
        ClientContext clientContext = new ClientContext(sid, transportType);
        sessions.put(sid, clientContext);
        return clientContext;
    }

    /**
     * 获取会话上下文。
     *
     * @param sessionId 会话 ID
     * @return 客户端上下文
     * @throws SessionNotFoundException 会话不存在时抛出
     */
    public ClientContext getSession(@NotNull String sessionId) {
        ClientContext context = sessions.get(sessionId);
        if (context == null) {
            throw new SessionNotFoundException(sessionId);
        }
        return context;
    }

    /**
     * 检查会话是否存在。
     *
     * @param sessionId 会话 ID
     * @return 会话是否存在
     */
    public boolean hasSession(@NotNull String sessionId) {
        return sessions.containsKey(sessionId);
    }

    /**
     * 移除会话及其超时任务。
     *
     * @param sessionId 会话 ID
     */
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        ScheduledFuture<?> task = sessionTimeoutTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * 调度会话超时任务。
     *
     * @param sessionId 会话 ID
     * @param onTimeout 超时回调
     */
    public void scheduleSessionTimeout(String sessionId, Runnable onTimeout) {
        ScheduledFuture<?> future = scheduler.schedule(onTimeout, pingTimeout, TimeUnit.MILLISECONDS);
        sessionTimeoutTasks.put(sessionId, future);
    }

    /**
     * 取消会话超时任务。
     *
     * @param sessionId 会话 ID
     */
    public void cancelSessionTimeout(String sessionId) {
        ScheduledFuture<?> task = sessionTimeoutTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * 更新会话的最后心跳时间。
     *
     * @param sessionId 会话 ID
     */
    public void updatePingTime(String sessionId) {
        ClientContext context = sessions.get(sessionId);
        if (context != null) {
            context.setLastPingTime(System.currentTimeMillis());
        }
    }

    public void setPingTimeout(long pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    public void setPingInterval(long pingInterval) {
        this.pingInterval = pingInterval;
    }

    public long getPingTimeout() {
        return pingTimeout;
    }

    public long getPingInterval() {
        return pingInterval;
    }

    /**
     * 获取当前活跃会话数。
     *
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 清除所有会话和超时任务（用于测试或关闭时）。
     */
    public void clear() {
        sessions.clear();
        sessionTimeoutTasks.values().forEach(task -> task.cancel(false));
        sessionTimeoutTasks.clear();
    }
}
