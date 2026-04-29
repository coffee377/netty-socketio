---
name: smart-commit
description: |
  智能 Git 提交技能。当用户需要提交代码变更、生成 Git Commit 信息、分组暂存文件时触发。

  使用场景：
  - 用户说"帮我提交"、"commit"、"提交代码"、"生成提交信息"、"分组提交"
  - 用户想按功能模块拆分提交，而不是一次性全部提交
  - 用户询问 git commit 最佳实践

  核心原则：仅处理已被 Git 追踪的文件变更，同功能同模块合并，跨功能跨类型拆分。

  注意：忽略未纳入版本管理的文件、临时配置、隐私无关文件。
---

# Smart Commit - 智能 Git 提交技能

## 核心规则

### 文件过滤
只处理已被 Git 追踪的文件变更（`git status` 中显示的 M/D 等状态）。

### 分组策略
| 场景 | 处理方式 |
|------|----------|
| 同功能同模块的零散变更 | **合并为一组**，不拆分 |
| 跨功能/跨模块 | **强制拆分**，每组单一职责 |
| 跨类型（feat + fix 混合） | **强制拆分**，按类型分开 |
| 同类型同模块 | **合并为一组** |

分组依据优先级：
1. **功能/模块**（同一功能的所有文件放一起）
2. **变更类型**（feat/fix/refactor/docs 等分开）

### 提交格式

```
<type>(<scope>): <简短标题>（50字内）

<详细描述（可选）>
```

### 类型列表
| Type | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构 |
| `docs` | 文档修改 |
| `style` | 代码格式（不影响功能） |
| `test` | 测试相关 |
| `perf` | 性能优化 |
| `chore` | 构建/工具/依赖变更 |

### Scope 规则
按模块/包名/组件名填写，例如：
- `feat(core)`: 核心模块
- `fix(auth)`: 认证模块
- `docs(api)`: API 文档

## 执行流程

1. **获取变更**：`git status` + `git diff --name-only`
2. **识别变更类型**：
   - `M` - 修改的文件（分析 diff 确定类型）
   - `D` - 删除的文件（根据文件路径和所属模块判断删除原因）
   - `A` - 新增的文件（根据文件名和用途判断类型）
3. **智能分组**：
   - 分析文件路径确定模块归属
   - **删除文件必须独立成组**，不与其他类型混合
   - 修改的文件按功能+类型双重维度分组
4. **生成输出**：
   - 先列出每组的文件清单
   - 再输出每组的标准 commit message
   - 删除文件组需要说明删除原因
5. **可直接执行**：输出的 commit 信息可直接复制使用

## 输出格式

### 分组清单
```
📦 分组 1: feat(engineio-netty) - WebSocket 协议处理器
  engineio-netty/src/.../ProtocolHandler.java
  engineio-netty/src/.../ChannelPipeline.java

📦 分组 2: fix(engineio-core) - 会话管理
  engineio-core/src/.../SessionManager.java
```

### Commit Messages
```bash
# 分组 1
git add engineio-netty/src/.../ProtocolHandler.java engineio-netty/src/.../ChannelPipeline.java
git commit -m "feat(engineio-netty): 实现动态 WebSocket 协议处理器集成

详细描述：
- 集成 WebSocketServerProtocolHandler 支持动态路由
- 优化 ChannelPipeline 配置"

# 分组 2
git add engineio-core/src/.../SessionManager.java
git commit -m "fix(engineio-core): 修复会话超时管理漏洞"
```

## 分组决策示例

```
假设变更文件：
  M auth-service/src/main/java/UserService.java      (feat)
  M auth-service/src/main/java/AuthController.java   (feat)
  M payment-service/src/main/java/PaymentService.java (fix)
  M auth-service/src/test/UserServiceTest.java       (test)
```

**正确分组：**
- 分组 1: `feat(auth-service)` - UserService.java + AuthController.java（同模块同类型，合并）
- 分组 2: `fix(payment-service)` - PaymentService.java（独立模块）
- 分组 3: `test(auth-service)` - UserServiceTest.java（不同类型，拆分）

## 删除文件处理

删除文件没有专门的 type 标识，需要根据**删除原因**选择合适的 type：

| 删除原因 | 推荐 Type | 示例 |
|----------|-----------|------|
| 重构/代码清理，移除废弃代码 | `refactor` | 移除过时的配置类 |
| 清理未使用的文件/脚本 | `chore` | 删除废弃的构建脚本 |
| 文档更新，移除过时文档 | `docs` | 删除废弃的 API 文档 |
| 测试文件清理 | `test` | 移除废弃的测试用例 |
| 功能移除（有意删除功能） | `feat` | 移除已废弃的登录功能 |

**分组决策**：
- 删除文件如果与其他变更在同一模块，**强制拆分**为独立提交
- 多个被删除文件同属一个模块且删除原因相同，可合并为一组
- 删除 + 新增同一文件（如替换实现）建议分开提交，便于 code review

## 注意事项

- **不拆分同功能内零散变更**：同一功能修改了 5 个文件，只要类型一致就合并
- **强制拆分跨类型**：feat 和 fix 即使在同一模块也要分开
- **强制拆分删除操作**：删除文件与其他变更必须分开提交
- **忽略无关文件**：.idea、.gradle、*.iml 等配置通常忽略