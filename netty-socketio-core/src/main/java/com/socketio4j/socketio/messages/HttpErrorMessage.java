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
package com.socketio4j.socketio.messages;

import java.util.Map;

/**
 * HTTP 错误消息，封装传输错误信息用于返回错误响应
 */
public class HttpErrorMessage extends HttpMessage {

    private final Map<String, Object> data;

    /**
     * 构造 HttpErrorMessage
     *
     * @param data 错误数据（包含 code、message 等字段）
     */
    public HttpErrorMessage(Map<String, Object> data) {
        super(null, null);
        this.data = data;
    }

    /**
     * 获取错误数据
     *
     * @return 错误数据映射
     */
    public Map<String, Object> getData() {
        return data;
    }
    
}
