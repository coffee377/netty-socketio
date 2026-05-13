package com.ccl.io.engine.core.session;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.EngineIOClient;
import com.ccl.io.engine.HandshakeData;
import com.ccl.io.engine.core.entity.ClientContext;
import com.ccl.io.engine.exception.SessionNotFoundException;
import com.ccl.io.engine.protocol.Transport;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final static Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final SessionManager INSTANCE = new SessionManager();

    private final ConcurrentHashMap<String, ClientContext> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EngineClient<HandshakeData>> clients = new ConcurrentHashMap<>();

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
     */
    public String createSession() {
        ClientContext session = createSession(Transport.POLLING);
        return session.getSessionId();
    }

    /**
     * 创建新会话。
     *
     * @param transportType 传输类型（polling 或 websocket）
     * @return 客户端上下文对象
     */
    @Deprecated
    public ClientContext createSession(Transport transportType) {
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

    public EngineClient<?> getClient(@NotNull String sessionId) {
        return clients.get(sessionId);
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

    /**
     * 设置心跳响应超时时间
     *
     * @param pingTimeout 超时时间（毫秒）
     */
    public void setPingTimeout(long pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    /**
     * 设置心跳发送间隔
     *
     * @param pingInterval 间隔时长（毫秒）
     */
    public void setPingInterval(long pingInterval) {
        this.pingInterval = pingInterval;
    }

    /**
     * 获取心跳响应超时时间
     *
     * @return 超时时间（毫秒）
     */
    public long getPingTimeout() {
        return pingTimeout;
    }

    /**
     * 获取心跳发送间隔
     *
     * @return 间隔时长（毫秒）
     */
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
