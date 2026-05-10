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
package com.socketio4j.socketio.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.socketio4j.socketio.namespace.Namespace;

/**
 * 注解扫描器接口
 *
 * <p>定义扫描注解、注册监听器、验证方法签名的统一 SPI
 */
public interface AnnotationScanner {

    /**
     * 获取要扫描的注解类型
     *
     * @return 注解类
     */
    Class<? extends Annotation> getScanAnnotation();

    /**
     * 根据注解将方法注册为事件监听器
     *
     * @param namespace  命名空间
     * @param object     监听器实例
     * @param method     注解方法
     * @param annotation 注解实例
     */
    void addListener(Namespace namespace, Object object, Method method, Annotation annotation);

    /**
     * 验证注解方法的签名是否合法
     *
     * @param method 注解方法
     * @param clazz  声明该方法的类
     * @throws IllegalArgumentException 签名不合法时抛出
     */
    void validate(Method method, Class<?> clazz);

}