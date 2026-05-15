package com.ccl.io.engine.netty.transport;

import io.netty.buffer.ByteBuf;

import java.util.Queue;

import com.ccl.io.engine.netty.transport.PollingTransport.PendingRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Polling 传输会话存储默认实现
 *
 * <p>使用 ConcurrentHashMap 存储每个 Session 的待发送消息队列和挂起的 GET 请求队列。
 * 支持并发访问，适用于多 EventLoop 线程场景。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0
 */
public class DefaultPollingSessionStore implements PollingSessionStore {

    private final ConcurrentHashMap<String, Queue<ByteBuf>> outputPackets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Queue<PendingRequest>> pendingGets = new ConcurrentHashMap<>();

    @Override
    public void enqueueOutput(String sessionId, ByteBuf data) {
        Queue<ByteBuf> queue = outputPackets.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        queue.offer(data);
    }

    @Override
    public ByteBuf pollOutput(String sessionId) {
        Queue<ByteBuf> queue = outputPackets.get(sessionId);
        return queue != null ? queue.poll() : null;
    }

    @Override
    public boolean hasOutput(String sessionId) {
        Queue<ByteBuf> queue = outputPackets.get(sessionId);
        return queue != null && !queue.isEmpty();
    }

    @Override
    public void registerPendingGet(String sessionId, PendingRequest request) {
        Queue<PendingRequest> queue = pendingGets.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        queue.offer(request);
    }

    @Override
    public PendingRequest pollPendingGet(String sessionId) {
        Queue<PendingRequest> queue = pendingGets.get(sessionId);
        return queue != null ? queue.poll() : null;
    }

    @Override
    public boolean hasPendingGet(String sessionId) {
        Queue<PendingRequest> queue = pendingGets.get(sessionId);
        return queue != null && !queue.isEmpty();
    }

    @Override
    public void removeSession(String sessionId) {
        outputPackets.remove(sessionId);
        Queue<PendingRequest> pending = pendingGets.remove(sessionId);
        if (pending != null) {
            PendingRequest request;
            while ((request = pending.poll()) != null) {
                request.getPromise().trySuccess();
            }
        }
    }
}
