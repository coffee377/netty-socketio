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
package com.socketio4j.socketio.ack;

import java.util.UUID;

import com.socketio4j.socketio.scheduler.SchedulerKey;

/**
 * ACK 调度器键，扩展 SchedulerKey 以包含 ACK 序号
 *
 * <p>用于唯一标识某个客户端某个 ACK 的超时调度任务
 */
public class AckSchedulerKey extends SchedulerKey {

    private final long index;

    /**
     * 构造 AckSchedulerKey 实例
     *
     * @param type      调度键类型
     * @param sessionId 客户端会话 ID
     * @param index     ACK 序号
     */
    public AckSchedulerKey(Type type, UUID sessionId, long index) {
        super(type, sessionId);
        this.index = index;
    }

    /**
     * 获取 ACK 序号
     *
     * @return ACK 序号
     */
    public long getIndex() {
        return index;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Long.hashCode(index);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        AckSchedulerKey other = (AckSchedulerKey) obj;
        if (index != other.index)
            return false;
        return true;
    }

}
