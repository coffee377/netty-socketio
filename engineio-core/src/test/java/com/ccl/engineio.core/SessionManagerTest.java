package com.ccl.engineio.core;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.core.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @BeforeEach
    void setUp() {
        SessionManager.getInstance().clear();
    }

    @Test
    void testCreateSession() {
        String sid = SessionManager.getInstance().createSession();
        assertNotNull(sid);
        assertFalse(sid.isEmpty());
    }

    @Test
    void testGetSession() {
        String sid = SessionManager.getInstance().createSession();
        ClientContext context = SessionManager.getInstance().getSession(sid);
        assertNotNull(context);
        assertEquals(sid, context.getSessionId());
    }

    @Test
    void testHasSession() {
        String sid = SessionManager.getInstance().createSession();
        assertTrue(SessionManager.getInstance().hasSession(sid));
        assertFalse(SessionManager.getInstance().hasSession("nonexistent"));
    }

    @Test
    void testRemoveSession() {
        String sid = SessionManager.getInstance().createSession();
        assertTrue(SessionManager.getInstance().hasSession(sid));
        
        SessionManager.getInstance().removeSession(sid);
        assertFalse(SessionManager.getInstance().hasSession(sid));
    }

    @Test
    void testUpdatePingTime() {
        String sid = SessionManager.getInstance().createSession();
        long before = System.currentTimeMillis();
        
        SessionManager.getInstance().updatePingTime(sid);
        
        ClientContext context = SessionManager.getInstance().getSession(sid);
        assertTrue(context.getLastPingTime() >= before);
    }

    @Test
    void testActiveSessionCount() {
        assertEquals(0, SessionManager.getInstance().getActiveSessionCount());
        
        SessionManager.getInstance().createSession();
        assertEquals(1, SessionManager.getInstance().getActiveSessionCount());
        
        SessionManager.getInstance().createSession();
        assertEquals(2, SessionManager.getInstance().getActiveSessionCount());
    }

    @Test
    void testConcurrentSessionCreation() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String sid = SessionManager.getInstance().createSession();
                    if (SessionManager.getInstance().hasSession(sid)) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertEquals(threadCount, successCount.get());
    }

    @Test
    void testConcurrentSessionAccess() throws InterruptedException {
        String sid = SessionManager.getInstance().createSession();
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ClientContext context = SessionManager.getInstance().getSession(sid);
                    if (context != null && context.getSessionId().equals(sid)) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Expected - session might be removed
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertEquals(threadCount, successCount.get());
    }

    @Test
    void testClear() {
        SessionManager.getInstance().createSession();
        SessionManager.getInstance().createSession();
        assertTrue(SessionManager.getInstance().getActiveSessionCount() > 0);
        
        SessionManager.getInstance().clear();
        assertEquals(0, SessionManager.getInstance().getActiveSessionCount());
    }
}