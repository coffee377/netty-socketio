---
name: chinese-javadoc
description: Use when 为 Java 代码添加或更新中文文档注释，确保类、字段、方法均包含 Javadoc 及 @param/@return/异常注解
---

# 中文 Javadoc 规范

## 概述

为 Java 代码添加标准化的中文 Javadoc 注释，遵循项目约定。

## 何时使用

- 为新 Java 类添加注释
- 为已有代码补充文档注释
- 代码评审要求使用中文注释

## 格式

### 类级别

```java
/**
 * 类的简要功能说明
 * <p>详细描述（可选）</p>
 */
public class Foo {
```

### 字段

```java
/** 字段说明 */
private String bar;
```

### 方法

```java
/**
 * 方法功能说明
 * <p>算法或业务逻辑细节（可选）</p>
 *
 * @param name 参数说明
 * @return 返回值说明
 * @throws XxxException 抛出条件
 */
public void method(int name) {
```

### 测试类

使用 `@Nested` 按功能分组，使用 `@DisplayName` 标注中文说明：

```java
@DisplayName("Foo 单元测试")
class FooTest {
    @Nested
    @DisplayName("基础操作")
    class Basic {
        @Test
        @DisplayName("满足条件 X 时应通过")
        void testX() { }
    }
}
```

## 原则

1. **注释为什么，而不是做什么** — 不言自明的代码不需要注释
2. **使用完整中文句子** — 避免简写和符号化表述
3. **单行注释不超过 40 个字符**
4. **方法的参数和返回值必须添加 @param/@return**
5. **测试类的 @DisplayName 使用中文**

## 检查清单

- [ ] 类级别 Javadoc 已添加
- [ ] 字段注释描述业务含义
- [ ] 方法注释包含功能和参数说明
- [ ] 测试类使用 @DisplayName 和 @Nested 分组
- [ ] 提交信息遵循 conventional commits 规范（例如 `docs(module): 为 X 添加 Javadoc`）
