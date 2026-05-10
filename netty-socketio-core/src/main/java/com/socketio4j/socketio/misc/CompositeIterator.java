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
package com.socketio4j.socketio.misc;

import java.util.Iterator;

/**
 * 复合迭代器，按顺序依次遍历多个子迭代器
 *
 * <p>当前子迭代器遍历完毕后自动切换到下一个，直到所有子迭代器耗尽
 *
 * @param <T> 元素类型
 */
public class CompositeIterator<T> implements Iterator<T> {

    private final Iterator<Iterator<T>> listIterator;
    private Iterator<T> currentIterator;

    /**
     * 构造复合迭代器
     *
     * @param listIterator 子迭代器的迭代器
     */
    public CompositeIterator(Iterator<Iterator<T>> listIterator) {
        this.currentIterator = null;
        this.listIterator = listIterator;
    }

    @Override
    public boolean hasNext() {
        if (currentIterator == null || !currentIterator.hasNext()) {
            while (listIterator.hasNext()) {
                Iterator<T> iterator = listIterator.next();
                if (iterator.hasNext()) {
                    currentIterator = iterator;
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public T next() {
        hasNext();
        return currentIterator.next();
    }

    @Override
    public void remove() {
        currentIterator.remove();
    }
}
