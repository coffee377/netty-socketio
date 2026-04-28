package com.ccl.socketio.core.ack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AckManager {

    private static final AckManager INSTANCE = new AckManager();
    private final Map<String, AckEntry> pendingAcks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private AckManager() {
        scheduler.scheduleAtFixedRate(this::cleanup, 30, 30, TimeUnit.SECONDS);
    }

    public static AckManager getInstance() {
        return INSTANCE;
    }

    public void registerAck(String ackId, Consumer<Object> callback, long timeout) {
        pendingAcks.put(ackId, new AckEntry(callback, System.currentTimeMillis() + timeout));
    }

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

    public void cancelAck(String ackId) {
        pendingAcks.remove(ackId);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        pendingAcks.entrySet().removeIf(entry -> entry.getValue().timeout < now);
    }

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

    private static class AckEntry {
        final Consumer<Object> callback;
        final long timeout;

        AckEntry(Consumer<Object> callback, long timeout) {
            this.callback = callback;
            this.timeout = timeout;
        }
    }
}