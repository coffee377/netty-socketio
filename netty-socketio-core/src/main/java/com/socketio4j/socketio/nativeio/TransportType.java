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
package com.socketio4j.socketio.nativeio;

/**
 * Netty 原生传输类型枚举
 *
 * <p>AUTO 自动选择最佳可用传输（优先级：io_uring -> epoll -> kqueue -> nio），
 * 也可手动指定具体实现
 */
public enum TransportType {
    /** 自动选择最佳可用传输 */
    AUTO,
    /** JVM 默认 NIO 传输 */
    NIO,
    /** Linux epoll 传输 */
    EPOLL,
    /** BSD/macOS kqueue 传输 */
    KQUEUE,
    /** Linux 5.1+ io_uring 传输，推荐生产环境 5.15+（LTS） */
    IO_URING;
}
