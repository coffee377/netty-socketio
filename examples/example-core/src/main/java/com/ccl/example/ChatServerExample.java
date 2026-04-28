package com.ccl.example;

import com.ccl.socketio.server.SocketIOServer;
import com.ccl.socketio.server.listener.SocketIOListener;

public class ChatServerExample {

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

        System.out.println("Starting Socket.IO Chat Server on port 3000...");
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            server.stop();
        }));
    }
}
