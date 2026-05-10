package com.ccl.engineio.netty.bootstrap;

/**
 * 服务端运行状态枚举
 *
 * <p>状态转换规则：
 * <ul>
 *   <li>INIT --start()--> STARTING --启动成功--> STARTED --stop()--> STOPPING --停止完成--> INIT</li>
 *   <li>INIT --start()--> STARTING --启动失败--> INIT</li>
 * </ul>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
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
