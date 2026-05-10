/**
 * Copyright (c) 2025 The Socketio4j Project
 * Parent project : Copyright (c) 2012-2025 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.socketio4j.socketio.listener;

import java.util.List;

import com.socketio4j.socketio.SocketIOClient;

import io.netty.channel.ChannelHandlerContext;

/**
 * 异常监听器，定义各生命周期阶段异常处理的回调接口
 */
public interface ExceptionListener {

    /**
     * 事件处理异常时调用
     *
     * @param e      异常
     * @param args   事件参数
     * @param client 客户端
     */
    void onEventException(Exception e, List<Object> args, SocketIOClient client);

    /**
     * 断开处理异常时调用
     *
     * @param e      异常
     * @param client 客户端
     */
    void onDisconnectException(Exception e, SocketIOClient client);

    /**
     * 连接处理异常时调用
     *
     * @param e      异常
     * @param client 客户端
     */
    void onConnectException(Exception e, SocketIOClient client);

    /**
     * Ping 处理异常时调用
     *
     * @deprecated 由 {@link #onPongException(Exception, SocketIOClient)} 替代
     * @param e      异常
     * @param client 客户端
     */
    @Deprecated
    void onPingException(Exception e, SocketIOClient client);

    /**
     * Pong 处理异常时调用
     *
     * @param e      异常
     * @param client 客户端
     */
    void onPongException(Exception e, SocketIOClient client);

    /**
     * Netty pipeline 异常捕获
     *
     * @param ctx ChannelHandlerContext
     * @param e   异常
     * @return 是否已处理
     */
    boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception;

    /**
     * 认证异常时调用
     *
     * @param e      异常
     * @param client 客户端
     */
    void onAuthException(Throwable e, SocketIOClient client);
}
