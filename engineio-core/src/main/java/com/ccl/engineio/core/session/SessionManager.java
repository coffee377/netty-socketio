package com.ccl.engineio.core.session;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.exception.SessionNotFoundException;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Engine.IO 会话管理器（单例）
 * 负责客户端会话的创建、查询、销毁和超时管理
 */
public class SessionManager {

    /** 单例实例 */
    private static final SessionManager INSTANCE = new SessionManager();

    /** 会话存储：sessionId -> 客户端上下文 */
    private final ConcurrentHashMap<String, ClientContext> sessions = new ConcurrentHashMap<>();

    /** 超时任务映射：sessionId -> 定时任务 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> sessionTimeoutTasks = new ConcurrentHashMap<>();

    /** 共享的调度线程池，避免每次创建超时任务时新建线程池 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** 心跳超时时间（毫秒），客户端在此时间内未响应则断开 */
    private long pingTimeout = 25000;

    /** 心跳间隔（毫秒），服务端发送 ping 的时间间隔 */
    private long pingInterval = 30000;

    /** 私有构造函数，防止外部实例化 */
    private SessionManager() {
    }

    /** 获取单例实例 */
    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * 创建新会话
     * @return 会话 ID（UUID，去掉连字符）
     */
    public String createSession() {
        String sid = UUID.randomUUID().toString().replace("-", "");
        ClientContext context = new ClientContext(sid);
        sessions.put(sid, context);
        return sid;
    }

    /**
     * 获取会话上下文（不存在时抛出异常）
     * @param sessionId 会话 ID
     * @return 客户端上下文
     * @throws SessionNotFoundException 会话不存在时抛出
     */
    public ClientContext getSession(String sessionId) {
        ClientContext context = sessions.get(sessionId);
        if (context == null) {
            throw new SessionNotFoundException(sessionId);
        }
        return context;
    }

    /**
     * 检查会话是否存在
     * @param sessionId 会话 ID
     * @return 会话是否存在
     */
    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    /**
     * 移除会话及其超时任务
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
     * 调度会话超时任务
     * 使用共享线程池，避免每次调用都创建新线程
     * @param sessionId 会话 ID
     * @param onTimeout 超时回调
     */
    public void scheduleSessionTimeout(String sessionId, Runnable onTimeout) {
        ScheduledFuture<?> future = scheduler.schedule(onTimeout, pingTimeout, TimeUnit.MILLISECONDS);
        sessionTimeoutTasks.put(sessionId, future);
    }

    /**
     * 取消会话超时任务
     * @param sessionId 会话 ID
     */
    public void cancelSessionTimeout(String sessionId) {
        ScheduledFuture<?> task = sessionTimeoutTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * 更新会话的最后心跳时间
     * @param sessionId 会话 ID
     */
    public void updatePingTime(String sessionId) {
        ClientContext context = sessions.get(sessionId);
        if (context != null) {
            context.setLastPingTime(System.currentTimeMillis());
        }
    }

    /** 设置心跳超时时间 */
    public void setPingTimeout(long pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    /** 设置心跳间隔 */
    public void setPingInterval(long pingInterval) {
        this.pingInterval = pingInterval;
    }

    /** 获取心跳超时时间 */
    public long getPingTimeout() {
        return pingTimeout;
    }

    /** 获取心跳间隔 */
    public long getPingInterval() {
        return pingInterval;
    }

    /**
     * 获取当前活跃会话数
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 清除所有会话和超时任务（用于测试或关闭时）
     */
    public void clear() {
        sessions.clear();
        sessionTimeoutTasks.values().forEach(task -> task.cancel(false));
        sessionTimeoutTasks.clear();
    }
}
