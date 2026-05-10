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
package com.socketio4j.socketio.protocol;


/**
 * Socket.IO 协议数据包类型枚举
 *
 * <p>外层类型（Engine.IO 级别）：OPEN, CLOSE, PING, PONG, MESSAGE, UPGRADE, NOOP
 * 内层类型（Socket.IO 级别）：CONNECT, DISCONNECT, EVENT, ACK, ERROR, BINARY_EVENT, BINARY_ACK
 */
public enum PacketType {

    OPEN(0), CLOSE(1), PING(2), PONG(3), MESSAGE(4), UPGRADE(5), NOOP(6),

    CONNECT(0, true), DISCONNECT(1, true), EVENT(2, true), ACK(3, true), ERROR(4, true), BINARY_EVENT(5, true), BINARY_ACK(6, true);

    public static final PacketType[] VALUES = values();
    private final int value;
    private final boolean inner;

    PacketType(int value) {
        this(value, false);
    }

    PacketType(int value, boolean inner) {
        this.value = value;
        this.inner = inner;
    }

    /**
     * 获取外层类型数值
     *
     * @return 类型数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 根据数值获取外层数据包类型
     *
     * @param value 类型数值
     * @return 对应的数据包类型
     * @throws IllegalStateException 数值不匹配时抛出
     */
    public static PacketType valueOf(int value) {
        for (PacketType type : VALUES) {
            if (type.getValue() == value && !type.inner) {
                return type;
            }
        }
        throw new IllegalStateException();
    }

    /**
     * 根据数值获取内层数据包类型
     *
     * @param value 类型数值
     * @return 对应的内层数据包类型
     * @throws IllegalArgumentException 数值不匹配时抛出
     */
    public static PacketType valueOfInner(int value) {
        for (PacketType type : VALUES) {
            if (type.getValue() == value && type.inner) {
                return type;
            }
        }
        throw new IllegalArgumentException("Can't parse " + value);
    }

}
