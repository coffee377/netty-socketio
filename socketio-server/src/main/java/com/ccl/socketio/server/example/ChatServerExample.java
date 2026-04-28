package com.ccl.socketio.server.example;

import com.ccl.socketio.server.SocketIOServer;
import com.ccl.socketio.server.listener.SocketIOListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatServerExample {

    private static final Logger log = LoggerFactory.getLogger(ChatServerExample.class);

    public static void main(String[] args) {
        SocketIOServer server = SocketIOServer.builder()
                .port(4000)
                .build();

        server.setListener(new SocketIOListener() {
            @Override
            public void onConnect(String sessionId, String namespace) {
                log.debug("Client connected: {} to {}", sessionId, namespace);
            }

            @Override
            public void onDisconnect(String sessionId, String namespace) {
                log.debug("Client disconnected: {} from {}", sessionId, namespace);
            }

            @Override
            public void onEvent(String sessionId, String namespace, String eventName, Object... args) {
                log.info("Event received: {} from {}", eventName, sessionId);
                for (Object arg : args) {
                    log.info("  - {}", arg.toString());
                }
            }

            @Override
            public void onError(String sessionId, String namespace, Throwable error) {
                log.error("Error for {}", sessionId, error);
            }
        });

        server.onEvent("/", "chat", (client) -> {
            log.debug("Chat event from: {}", client.getSessionId());
        });

        server.onEvent("/", "join", (client) -> {
            log.debug("Join event from: {}", client.getSessionId());
        });

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop();
        }));
    }
}
