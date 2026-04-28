package com.ccl.engineio.core.session;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.exception.SessionNotFoundException;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final ConcurrentHashMap<String, ClientContext> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> sessionTimeoutTasks = new ConcurrentHashMap<>();

    private long pingTimeout = 25000;
    private long pingInterval = 30000;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession() {
        String sid = UUID.randomUUID().toString().replace("-", "");
        ClientContext context = new ClientContext(sid);
        sessions.put(sid, context);
        return sid;
    }

    public ClientContext getSession(String sessionId) {
        ClientContext context = sessions.get(sessionId);
        if (context == null) {
            throw new SessionNotFoundException(sessionId);
        }
        return context;
    }

    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        ScheduledFuture<?> task = sessionTimeoutTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
    }

    public void scheduleSessionTimeout(String sessionId, Runnable onTimeout) {
        ScheduledFuture<?> future = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                .schedule(onTimeout, pingTimeout, TimeUnit.MILLISECONDS);
        sessionTimeoutTasks.put(sessionId, future);
    }

    public void cancelSessionTimeout(String sessionId) {
        ScheduledFuture<?> task = sessionTimeoutTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
    }

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

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public void clear() {
        sessions.clear();
        sessionTimeoutTasks.values().forEach(task -> task.cancel(false));
        sessionTimeoutTasks.clear();
    }
}
