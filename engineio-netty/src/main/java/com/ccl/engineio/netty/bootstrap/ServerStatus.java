package com.ccl.engineio.netty.bootstrap;

/**
 * Server status enum.
 * Transitions:
 * INIT --start()--> STARTING --start() successfully--> STARTED --stop()--> STOPPING --stop() finished--> INIT
 * INIT --start()--> STARTING --start() failed--> INIT
 */
public enum ServerStatus {
    /**
     * SocketIOServer is created.
     * Or start() failed.
     * Or stop() is finished(either successfully or failed).
     */
    INIT,
    /**
     * SocketIOServer.start() is called, but not finished yet.
     */
    STARTING,
    /**
     * SocketIOServer is started and running.
     */
    STARTED,
    /**
     * SocketIOServer.stop() is called, but not finished yet.
     */
    STOPPING;
}
