---
name: java-comment
description: |
  Java 代码中文注释技能。用于为 Java 代码添加符合规范的中文 Javadoc 文档注释、
  正确使用注解（@Override、@Deprecated 等），以及审查和改进现有代码的注释质量。

  当用户提到"添加注释"、"写 Javadoc"、"给这个方法加文档"、"完善注解"、
  "审查代码注释"、"改进这个类的文档"、或者涉及 @param、@return、@DisplayName
  等 Javadoc 标签或注解时，触发此技能。

  注意：如果只是简单的代码补全或重构，不涉及注释/文档改进，则不需要触发。
---

# Java 中文注释规范与实践

本技能帮助生成符合规范的中文 Javadoc、使用恰当的注解，以及改进现有代码的注释质量。

## Javadoc 核心原则

### 何时写 Javadoc

**必须撰写 Javadoc 的情况：**
- 所有 `public` 和 `protected` 的类、方法、字段
- 涉及业务逻辑的核心类
- 供外部使用的 API

**可选但推荐的情况：**
- 包级私有类的公开方法（如果有的话）
- 复杂算法或有特殊业务含义的代码

**不应撰写冗余 Javadoc 的情况：**
- getter/setter（除非有特殊业务含义）
- private 方法（除非逻辑复杂需要说明）
- 明显自解释的代码

### Javadoc 结构规范

```java
/**
 * 方法的简短描述（简洁、清晰，1-2 句话）
 *
 * <p>可选的详细说明段落，用于解释：
 * <ul>
 *   <li>业务背景或设计决策</li>
 *   <li>使用场景和示例</li>
 *   <li>与相关方法的关联</li>
 * </ul>
 *
 * @param paramName 参数说明（简洁、清晰）
 * @return 返回值说明（不能是 void）
 * @throws ExceptionName 异常说明（何时抛出）
 * @see #relatedMethod() 关联方法引用
 */
```

**关键规则：**
1. 首句必须是可以独立存在的简短描述
2. 使用 `<p>` 分隔多个段落
3. `@param`、`@return`、`@throws` 按固定顺序排列
4. 参数名使用 `paramName` 格式（非 `pParamName` 或 `mParamName`）
5. 对于 boolean 参数，说明"是/否"状态的含义

### 常见标签使用

| 标签 | 使用场景 | 示例 |
|------|----------|------|
| `@param` | 所有非 void 方法的参数 | `@param username 登录用户名` |
| `@return` | 所有非 void 方法 | `@return 会话 ID，null 表示未建立会话` |
| `@throws` | 可能抛出的受检异常 | `@throws IOException 当网络连接失败时` |
| `@see` | 关联方法或类 | `@see #process(String)` |
| `@since` | API 版本 | `@since 1.0.0` |
| `@deprecated` | 已废弃 API | 配合 `@deprecated` 注释使用 |

**不应使用 `@return void`**，如果方法返回 void，直接省略此标签。

### 注释风格

- **描述行为，而非实现**：说明"做什么"而非"怎么做"
- **简洁明了**：避免显而易见的描述，如 `// 增加 i`
- **使用第三人称**：Javadoc 使用陈述句，而非"获取用户信息"而是"获取用户信息"
- **避免无意义注释**：不要写 `// 循环` 这类废话

## 注解（Annotation）使用规范

### 标准注解

| 注解 | 用途 | 使用建议 |
|------|------|----------|
| `@Override` | **必须使用** | 重写父类/接口方法时强制添加 |
| `@Deprecated` | 标记废弃 | 同时添加 Javadoc `@deprecated` 说明替代方案 |
| `@SuppressWarnings` | 抑制警告 | 需注明原因：`@SuppressWarnings("unchecked") // 泛型类型推断` |
| `@SafeVarargs` | 安全的可变参数 | 与泛型可变参数方法配合使用 |

### 常见错误

1. **过度使用 `@Deprecated`**
   ```java
   // 错误：只是标记，不说明替代方案
   @Deprecated
   public void oldMethod() {}

   // 正确：说明替代方案
   /**
    * @deprecated Use {@link #newMethod(String)} instead
    */
   @Deprecated
   public void oldMethod() {}
   ```

2. **滥用 `@SuppressWarnings`**
   ```java
   // 错误：不说明原因
   @SuppressWarnings("unchecked")
   private Object convert() {}

   // 正确：说明为何需要抑制
   @SuppressWarnings("unchecked")
   private List<User> getUsers() {
       // 内部类型推断保证安全
       return (List<User>) userMap.get("users");
   }
   ```

## 改进现有注释

### 审查清单

1. **Javadoc 是否完整？**
   - 公开 API 是否有文档？
   - 参数、返回值、异常是否说明？

2. **注释是否准确？**
   - 注释描述与实际代码是否一致？
   - 是否有过时的注释？

3. **注释风格是否规范？**
   - 是否使用 /** */ 而非 /* */
   - 是否避免无意义注释？

### 改进示例

**问题代码：**
```java
/**
 * 获取用户
 */
public User getUser(String id) {
    // 检查 null
    if (id == null) {
        return null;
    }
    return userMap.get(id);
}
```

**改进后：**
```java
/**
 * 根据用户 ID 获取用户信息
 *
 * @param id 用户唯一标识符
 * @return 用户对象，不存在时返回 null
 */
public User getUser(String id) {
    if (id == null) {
        return null;
    }
    return userMap.get(id);
}
```

## 生成新注释的流程

1. **分析代码意图**
   - 这是什么功能？
   - 公开还是内部使用？
   - 有哪些输入输出？

2. **确定注释级别**
   - 公开 API → 完整 Javadoc
   - 内部实现 → 简洁注释或省略

3. **撰写 Javadoc**
   - 首句描述核心功能
   - 补充必要的业务背景
   - 添加 @param/@return/@throws

4. **检查注解使用**
   - 重写方法是否标注 @Override？
   - 废弃方法是否配合 @deprecated 说明？

## 参考格式

### 类文档
```java
/**
 * [类名] 负责...
 *
 * <p>设计决策或使用场景说明：
 * - 核心职责
 * - 与其他组件的关系
 *
 * @author 作者名
 * @since 1.0.0
 */
public class XxxService {}
```

### 方法文档
```java
/**
 * [动词]...
 *
 * @param xxx 参数说明
 * @return 返回值说明（void 时省略）
 * @throws XxxException 异常条件
 */
public ReturnType methodName(Type xxx) {}
```

## 测试类规范

JUnit 5 测试类使用 `@DisplayName` 和 `@Nested` 组织中文测试：

```java
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Nested
    @DisplayName("基础操作")
    class BasicOperations {
        @Test
        @DisplayName("根据 ID 查询用户，返回用户对象")
        void findById_returnsUser() { }

        @Test
        @DisplayName("ID 为空时返回 null")
        void findById_nullId_returnsNull() { }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateOperations {
        @Test
        @DisplayName("更新用户信息成功")
        void updateUser_success() { }
    }
}
```

**规则：**
- 类级别使用 `@DisplayName` 描述测试目标
- 使用 `@Nested` 按功能分组测试
- `@DisplayName` 使用完整的描述性中文句子

## 中文注释原则

1. **使用完整中文句子** — 避免简写和符号化表述
2. **注释结尾不使用句号** — 简洁风格，不添加句号
3. **单行注释不超过 40 个字符**
4. **注释说明"为什么"，而非"做什么"**
5. **所有 Javadoc 必须使用多行块注释 `/** */`**，禁止单行格式

```java
// 错误：单行 Javadoc
/** 字段说明 */
private String field;

// 正确：多行 Javadoc
/**
 * 字段说明
 */
private String field;
```

## 检查清单

完成注释后，对照检查：

- [ ] 类级别 Javadoc 已添加（public/protected）
- [ ] 字段使用多行块注释，禁止单行 `/** */` 或 `//`
- [ ] 中文注释结尾不使用句号
- [ ] 方法注释包含功能和参数说明
- [ ] `@param`、`@return` 标签完整（void 方法除外）
- [ ] 重写方法标注了 `@Override`
- [ ] 废弃 API 配合 `@deprecated` 说明替代方案
- [ ] 测试类使用 `@DisplayName` 和 `@Nested` 分组
- [ ] 避免了无意义注释（如 `// 检查 null`）
- [ ] 提交信息遵循规范（`docs(module): 为 X 添加 Javadoc`）
