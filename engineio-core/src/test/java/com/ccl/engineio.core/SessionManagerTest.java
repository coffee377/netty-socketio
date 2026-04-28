package com.ccl.engineio.core;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.core.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionManager 单元测试
 * 覆盖基础操作和并发安全性测试
 */
@DisplayName("SessionManager 单元测试")
class SessionManagerTest {

    @BeforeEach
    void setUp() {
        SessionManager.getInstance().clear();
    }

    @Nested
    @DisplayName("基础会话操作")
    class BasicOperations {

        @Test
        @DisplayName("创建会话应返回非空 ID")
        void testCreateSession() {
            String sid = SessionManager.getInstance().createSession();
            assertNotNull(sid);
            assertFalse(sid.isEmpty());
        }

        @Test
        @DisplayName("应能获取创建的会话")
        void testGetSession() {
            String sid = SessionManager.getInstance().createSession();
            ClientContext context = SessionManager.getInstance().getSession(sid);
            assertNotNull(context);
            assertEquals(sid, context.getSessionId());
        }

        @Test
        @DisplayName("应能正确检查会话是否存在")
        void testHasSession() {
            String sid = SessionManager.getInstance().createSession();
            assertTrue(SessionManager.getInstance().hasSession(sid));
            assertFalse(SessionManager.getInstance().hasSession("nonexistent"));
        }

        @Test
        @DisplayName("移除会话后不应存在")
        void testRemoveSession() {
            String sid = SessionManager.getInstance().createSession();
            assertTrue(SessionManager.getInstance().hasSession(sid));

            SessionManager.getInstance().removeSession(sid);
            assertFalse(SessionManager.getInstance().hasSession(sid));
        }

        @Test
        @DisplayName("更新心跳时间应记录当前时间")
        void testUpdatePingTime() {
            String sid = SessionManager.getInstance().createSession();
            long before = System.currentTimeMillis();

            SessionManager.getInstance().updatePingTime(sid);

            ClientContext context = SessionManager.getInstance().getSession(sid);
            assertTrue(context.getLastPingTime() >= before);
        }

        @Test
        @DisplayName("活跃会话计数应准确")
        void testActiveSessionCount() {
            assertEquals(0, SessionManager.getInstance().getActiveSessionCount());

            SessionManager.getInstance().createSession();
            assertEquals(1, SessionManager.getInstance().getActiveSessionCount());

            SessionManager.getInstance().createSession();
            assertEquals(2, SessionManager.getInstance().getActiveSessionCount());
        }

        @Test
        @DisplayName("清除会话应重置所有状态")
        void testClear() {
            SessionManager.getInstance().createSession();
            SessionManager.getInstance().createSession();
            assertTrue(SessionManager.getInstance().getActiveSessionCount() > 0);

            SessionManager.getInstance().clear();
            assertEquals(0, SessionManager.getInstance().getActiveSessionCount());
        }
    }

    @Nested
    @DisplayName("并发安全性")
    class ConcurrentSafety {

        @Test
        @DisplayName("并发创建会话不应丢失")
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
        @DisplayName("并发访问同一会话不应抛出异常")
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
                        // 异常：会话可能在测试期间被移除
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(threadCount, successCount.get());
        }
    }
}