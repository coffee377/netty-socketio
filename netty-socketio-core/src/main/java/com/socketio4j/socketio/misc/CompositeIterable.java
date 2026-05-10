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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 复合 Iterable，将多个 Iterable 组合为一个统一的 Iterable
 *
 * <p>支持数组和 List 两种构造方式，迭代时按顺序依次遍历所有子 Iterable
 *
 * @param <T> 元素类型
 */
public class CompositeIterable<T> implements Iterable<T> {

    private List<Iterable<T>> iterablesList;
    private Iterable<T>[] iterables;

    /**
     * 使用 List 构造复合 Iterable
     *
     * @param iterables 子 Iterable 列表
     */
    public CompositeIterable(List<Iterable<T>> iterables) {
        this.iterablesList = iterables;
    }

    /**
     * 使用数组构造复合 Iterable
     *
     * @param iterables 子 Iterable 数组
     */
    @SafeVarargs
    public CompositeIterable(Iterable<T>... iterables) {
        this.iterables = iterables;
    }

    /**
     * 拷贝构造，引用同一组子 Iterable
     *
     * @param iterable 源复合 Iterable
     */
    public CompositeIterable(CompositeIterable<T> iterable) {
        this.iterables = iterable.iterables;
        this.iterablesList = iterable.iterablesList;
    }

    @Override
    public Iterator<T> iterator() {
        List<Iterator<T>> iterators = new ArrayList<Iterator<T>>();
        if (iterables != null) {
            for (Iterable<T> iterable : iterables) {
                iterators.add(iterable.iterator());
            }
        } else {
            for (Iterable<T> iterable : iterablesList) {
                iterators.add(iterable.iterator());
            }
        }
        return new CompositeIterator<T>(iterators.iterator());
    }


}
