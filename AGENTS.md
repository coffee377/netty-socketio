# netty-socketio (重构版)

基于 Netty 的 Java Socket.IO 服务端实现，重构中的版本。

## 快速开始

    ./gradlew build                              # 构建所有模块
    ./gradlew test                               # 运行所有测试
    ./gradlew :<模块名>:test                      # 运行指定模块测试
    ./gradlew :<模块名>:test --tests "*TestName"  # 运行单个测试
    ./gradlew :socketio-server:run               # 运行 ChatServerExample 示例

## 模块结构

| 模块 | 包名 | 描述 |
|------|------|------|
| `engineio-core` | `com.ccl.engineio` | Engine.IO 协议核心（解析器、Session 管理） |
| `engineio-netty` | `com.ccl.engineio.netty` | Engine.IO 的 Netty 传输层 |
| `socketio-core` | `com.ccl.socketio.core` | Socket.IO 协议层 |
| `socketio-netty` | `com.ccl.socketio.netty` | Socket.IO 的 Netty 传输层（Codec、Handler、Pipeline） |
| `socketio-server` | `com.ccl.socketio.server` | 服务端 Bootstrap，主入口 |
| `examples:example-core` | `com.ccl.example` | 示例应用 |

依赖关系：

```
socketio-server → socketio-netty → socketio-core   → engineio-core
                 → engineio-netty ──────────────────┘
```

## 构建配置

- **Gradle 9.4.0**，Kotlin DSL
- **Java 8** 目标（`options.release.set(8)`）
- 并行构建 + 配置缓存已启用（`gradle.properties`）
- 版本目录：`gradle/libs.versions.toml`
- 编译器开启 `-Xlint:unchecked` 和 `-Xlint:deprecation`
- 版本：`4.0.0-alpha.0`

## 测试

- JUnit 5（Jupiter + Vintage 兼容 JUnit 4）
- 测试过滤器：仅匹配 `*Test` 和 `*Tests`
- 根 `build.gradle.kts` 为 Test 配置了 `--add-opens` JVM 参数（`java.lang`、`java.util`、`java.lang.reflect`）

## Notes

- `socketio-server` 模块配置了 `application` 插件，主类为 `com.ccl.socketio.server.example.ChatServerExample`
- 暂无 CI 工作流
- Maven 发布已配置，但属于基础设施层面，暂不需要关注
