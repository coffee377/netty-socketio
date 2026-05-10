package com.ccl.example;

import com.ccl.socketio.server.SocketIOServer;
import com.ccl.socketio.server.listener.SocketIOListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socket.IO 聊天服务器示例
 *
 * <p>演示使用重构版 {@link com.ccl.socketio.server.SocketIOServer} 启动服务，
 * 通过 {@link com.ccl.socketio.server.listener.SocketIOListener} 处理连接、事件和错误。
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public class ChatServerExample {

    private final static Logger log = LoggerFactory.getLogger(ChatServerExample.class);

    public static void main(String[] args) {
        SocketIOServer server = SocketIOServer.builder()
                .port(4000)
                .build();

        server.setListener(new SocketIOListener() {
            @Override
            public void onConnect(String sessionId, String namespace) {
                System.out.println("Client connected: " + sessionId + " to " + namespace);
            }

            @Override
            public void onDisconnect(String sessionId, String namespace) {
                System.out.println("Client disconnected: " + sessionId + " from " + namespace);
            }

            @Override
            public void onEvent(String sessionId, String namespace, String eventName, Object... args) {
                System.out.println("Event received: " + eventName + " from " + sessionId);
                for (Object arg : args) {
                    System.out.println("  - " + arg);
                }
            }

            @Override
            public void onError(String sessionId, String namespace, Throwable error) {
                System.err.println("Error for " + sessionId + ": " + error.getMessage());
            }
        });

        log.info("Starting Socket.IO Chat Server on http://localhost:{} ...", 4000);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop();
        }));
    }
}
