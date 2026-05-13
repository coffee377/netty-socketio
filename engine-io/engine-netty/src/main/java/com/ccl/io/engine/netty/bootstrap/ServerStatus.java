package com.ccl.io.engine.netty.bootstrap;

public enum ServerStatus {
    /**
     * SocketIOServer 已创建，或 start() 失败，或 stop() 已完成（成功或失败）
     */
    INIT,
    /**
     * SocketIOServer.start() 已调用，但尚未完成
     */
    STARTING,
    /**
     * SocketIOServer 已启动并正在运行
     */
    STARTED,
    /**
     * SocketIOServer.stop() 已调用，但尚未完成
     */
    STOPPING;
}
