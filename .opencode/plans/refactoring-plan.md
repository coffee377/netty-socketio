# netty-socketio 重构计划

> 基于当前代码现状编写 ｜ 2026-04-28

## 1. 背景

`netty-socketio-core` 是原版本（单体模块），重构目标是拆分为分层架构的六个模块：

```
socketio-server → socketio-netty → socketio-core   → engineio-core
                 → engineio-netty ──────────────────┘
                    (旧) netty-socketio-core ── 待废弃
```

**目标状态：** 新模块可独立运行、通过测试、覆盖旧模块核心功能，最终废弃 `netty-socketio-core`。

---

## 2. 当前状态总览

| 模块 | 状态 | 核心问题 |
|------|------|----------|
| `engineio-core` | 核心逻辑完成 | stale tests, OpenData 不完整, SessionManager 线程池泄漏 |
| `engineio-netty` | 骨架阶段 | 多数 handler 为空实现, PipelineFactory 不完整, 无 ServerBootstrap |
| `socketio-core` | 核心完成 | SocketIODecoder 不完整, SocketIOClient 方法为空, 缺 Room 支持, BinaryAttachment 格式错误 |
| `socketio-netty` | Handler 已写未接入 | SocketIOCodec 是 stub, Pipeline 中 Socket.IO 层全部注释 |
| `socketio-server` | API 完成 | 端口硬编码, 配置传递链路未打通 |
| `netty-socketio-core` | 旧版本(待废弃) | 功能完整, 是参考实现 |

---

## 3. 层级架构设计

### 3.1 协议分层

```
┌─────────────────────────────────────────────────────────────┐
│ L4: Application API (socketio-server)                       │
│     SocketIOServer, SocketIOListener, ServerConfig          │
├─────────────────────────────────────────────────────────────┤
│ L3: Socket.IO Transport (socketio-netty)                    │
│     Netty Pipeline, SocketIOCodec, Namespace/Event Handler  │
├─────────────────────────────────────────────────────────────┤
│ L2: Socket.IO Protocol (socketio-core)                      │
│     SocketPacket, Codec, Namespace/Room/ACK Management      │
├─────────────────────────────────────────────────────────────┤
│ L1: Engine.IO Transport (engineio-netty)                    │
│     HTTP/WebSocket, Handshake, Heartbeat, Polling           │
├─────────────────────────────────────────────────────────────┤
│ L0: Engine.IO Protocol (engineio-core)                      │
│     EngineIOPacket, Parser, Session, Transport Types        │
├─────────────────────────────────────────────────────────────┤
│ Netty (TCP, HTTP Codec, WebSocket Protocol Handler)         │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 数据流（入站）

```
TCP Frame
  → HTTP Request (HttpServerCodec)
  → Engine.IO Handshake (创建 Session, 响应 OPEN)
  → Transport Router (POLLING / WEBSOCKET)
  → WebSocket Frame (EngineIOUpgradeHandler → ByteBuf)
  → Engine.IO Packet (EngineIOCodec: ByteBuf → EngineIOPacket)
  → Socket.IO Packet (SocketIOCodec: EngineIOPacket<String> → SocketPacket)
  → Namespace Handler (CONNECT/EVENT/ACK 处理)
  → Event Router (路由到业务 handler)
  → Business Handler (用户回调)
```

### 3.3 数据流（出站）

```
SocketPacket (业务逻辑创建)
  → SocketIOCodec 编码为 String
  → EngineIOCodec 包装为 EngineIOPacket(Type.MESSAGE, string)
  → 根据 Transport 类型发送:
    - WebSocket: TextWebSocketFrame
    - Polling: HTTP Response body
  二进制事件:
    - TextWebSocketFrame ("5A-count[/ns][...]"  +  binary frames)
```

---

## 4. 实施计划

按从底层到上层的顺序实施，每层完成后验证通过再继续上层。

### Phase 0: 清理和基础修复

**P0-1: 删除 stale test 文件**
- 删除 `engineio-core/src/test/java/com/ccl/engineio.core/EnginePacketTest.java`
- 删除 `engineio-core/src/test/java/com/ccl/engineio.core/EngineIODecoderTest.java`
- 删除 `engineio-core/src/test/java/com/ccl/engineio.core/EngineIOEncoderTest.java`
- 这几个测试引用了不存在的类（`EnginePacket`, `EngineIODecoder`, `EngineIOEncoder`），无法编译

**P0-2: 修复 OpenData**
- 文件: `engineio-core/src/main/java/.../entity/OpenData.java`
- 问题: 无 getter/setter, 字段 `getPingInterval` 命名错误（与已有字段 `pingInterval` 重复）
- 修复: 添加完整 getter/setter, 删除 `getPingInterval` 字段, 确保字段名与 Engine.IO 协议一致

**P0-3: 修复 SessionManager 线程池泄漏**
- 文件: `engineio-core/src/main/java/.../session/SessionManager.java`
- 问题: `scheduleSessionTimeout()` 每次创建 `newSingleThreadScheduledExecutor()`
- 修复: 使用共享的 `ScheduledExecutorService`（在构造函数中创建）

**验证:** `./gradlew :engineio-core:test` 全部通过

---

### Phase 1: engineio-netty — 完成传输层

这是最关键的一层。当前只有骨架，需要实现完整的 Engine.IO over Netty。

**P1-1: 实现 EngineIOHandshakeHandler**
- 文件: `engineio-netty/.../handler/EngineIOHandshakeHandler.java`
- 功能:
  1. 处理 `FullHttpRequest`，解析 URL 参数（`transport`, `sid`, `EIO` 等）
  2. 首次连接（无 `sid`）: 调用 `SessionManager.createSession()`，构建 `OpenData`，发送 `OPEN` 包（`0{...}`），将 sessionId 写入 `ChannelAttributes.SESSION_ID`
  3. 已存在 session: 验证 session，更新 `ClientContext`
  4. 将原始 HttpRequest 存储到 `ChannelAttributes.HTTP_REQUEST`
- 参考: 旧版本 `AuthorizeHandler.java` + `InPacketHandler.java` 的握手逻辑

**P1-2: 实现 EngineIOCodec — 入站解码 + 出站编码**
- 文件: `engineio-netty/.../handler/EngineIOCodec.java`
- 入站 (`channelRead`):
  - 接收 `ByteBuf`（来自 WebSocket upgrade）或 `FullHttpRequest`（来自 polling）
  - 提取原始数据（String 或 byte[]）
  - 调用 `ParserV4.decodePacket()` 解析为 `EngineIOPacket<?>`
  - 处理 Packet 类型:
    - `PING`: 回复 `PONG`
    - `PONG`: 取消 session timeout
    - `MESSAGE`: 提取 String payload，向下游传递
    - `CLOSE`: 关闭 session 连接
    - `UPGRADE`: 确认升级
- 出站 (`write`):
  - `EngineIOPacket<?>` → 调用 `ParserV4.encodePacket()` → 编码为 `TextWebSocketFrame` 或 HTTP response
  - 根据 Channel 上的 transport 类型决定输出格式

**P1-3: 实现 EngineIOUpgradeHandler — WebSocket 升级**
- 文件: `engineio-netty/.../handler/EngineIOUpgradeHandler.java`
- 当前问题:
  1. `WebSocketServerProtocolHandler` 未加入 pipeline
  2. `userEventTriggered` 中升级逻辑为空
- 修复:
  1. 在 handshake handler 中，对 `transport=websocket` 的请求动态添加 `WebSocketServerProtocolHandler`
  2. WebSocket 握手完成后，切换到 WebSocket 传输模式
  3. 处理 Engine.IO 升级协议（`UPGRADE` / `NOOP` packet）

**P1-4: 重构 PipelineFactory**
- 文件: `engineio-netty/.../pipeline/PipelineFactory.java`
- 完整的 pipeline 应该包含:
  ```
  HttpServerCodec
  HttpObjectAggregator(maxFramePayloadLength)
  EngineIOHandshakeHandler     — 握手 + Session 创建
  EngineIOCodec                — 编解码 (Duplex)
  EngineIOHeartbeatHandler     — 心跳
  EngineIOSessionHandler       — Session 生命周期
  ```
- 注意: `WebSocketServerProtocolHandler` 应在握手后动态添加，不在初始 pipeline 中

**P1-5: 实现 Polling Transport**
- 新增: `engineio-netty/.../transport/PollingHandler.java`
- 功能:
  1. 接收 HTTP 请求，解析 Engine.IO payload
  2. 响应 HTTP 请求，编码 Engine.IO packet
  3. 长轮询: 持有没有消息的连接，有新消息时释放
  4. 支持 payload 长度分隔格式（`length-data`）

**P1-6: 整合 WebSocketServerProtocolHandler**
- 需要在握手成功后，动态修改 pipeline:
  1. 添加 `WebSocketServerProtocolHandler`
  2. 触发 WebSocket 握手
  3. 握手完成后移除 HTTP handler（可选），保留 Engine.IO handler

**验证:** `./gradlew :engineio-netty:test` + 集成测试验证 WebSocket 连接和心跳

---

### Phase 2: socketio-core — 完成协议层

**P2-1: 完善 SocketIODecoder**
- 文件: `socketio-core/.../codec/SocketIODecoder.java`
- 当前问题: 只解析 type 和 namespace，未解析 event name、data、ack
- 需要实现:
  1. EVENT/BINARY_EVENT: 解析 `eventName`（引号包裹的字符串）和 `data`（JSON 数组/对象）
  2. ACK/BINARY_ACK: 解析 `ackId` 和 `data`（`+` 后跟随 JSON）
  3. CONNECT: 解析可选的 JSON auth payload
  4. 二进制附件标记解析: 检测 JSON 中是否包含 `{"_placeholder":true,"num":N}`，若有则标记需要等待二进制帧
  5. 解析 BINARY_EVENT/BINARY_ACK 的 `A` 标记（如 `53-` 表示 3 个二进制附件）
- 参考 Socket.IO v4 协议格式:
  ```
  0[/namespace][{auth}]                — CONNECT
  1[/namespace]                        — DISCONNECT
  2[/namespace]["event"][,data]        — EVENT
  3[/namespace][ackId][,+data]         — ACK
  4                                    — ERROR
  5[n]Acount[/namespace]["event"]      — BINARY_EVENT (n = 无A标记时的附件数)
  6[n]Acount[/namespace][ackId]        — BINARY_ACK
  ```
- 二进制检测: 解析 JSON 数据时扫描 `"_placeholder":true`，若有则设置 `SocketPacket.hasBinary = true` 并记录附件数量

**P2-2: 完善 SocketIOEncoder — 二进制支持**
- 文件: `socketio-core/.../codec/SocketIOEncoder.java`
- 当前问题: 编码器已支持 text，但缺少二进制附件处理
- **二进制占位符机制（Socket.IO 标准格式）**：
  1. 编码 BINARY_EVENT 时，数据中的 `byte[]` 在 JSON 中替换为 `{"_placeholder":true,"num":N}`（N 从 0 开始）
  2. 实际二进制数据不在 JSON 中，而是作为独立的 WebSocket BinaryFrame 后续发送
  3. 参考旧版本 `JacksonJsonSupport.ByteArraySerializer` 实现
  
- **需要实现:**
  1. BINARY_EVENT/BINARY_ACK: 在 JSON 序列化 `byte[]` 时输出 `{"_placeholder":true,"num":N}`
  2. 收集二进制 attachment 到 `SocketPacket.binaryAttachments`
  3. 编码器需返回文本部分（含占位符的 JSON）和二进制 attachment 列表，由上层 Handler 分别发送
  4. 协议格式: `5Acount[/namespace]["event",{"_placeholder":true,"num":0},...]`
  
- **`BinaryAttachment` 重设计：**
  - 当前 `___BINARY0___BINARY1___` 格式与 Socket.IO 不兼容，需改为标准占位符
  - `createPlaceholderJson(int index)` → 返回 `{"_placeholder":true,"num":<index>}`
  - 保留 `addAttachment(byte[])` 和 `getAllAttachments()`
  - `replacePlaceholders()` 用于解码端: 将 JSON 中的 `{"_placeholder":true,"num":N}` 替换为 Base64 编码的二进制数据

**P2-3: 实现 Room 管理**
- 新增: `socketio-core/.../room/RoomManager.java`
- 功能:
  1. `join(sessionId, room)` / `leave(sessionId, room)` / `allJoin` / `allLeave`
  2. `getRooms(sessionId)` 返回客户端加入的房间列表
  3. `getClients(room)` 返回房间内的客户端列表
  4. 默认房间: 每个客户端自动加入以自身 sessionId 命名的房间
  5. 线程安全（`ConcurrentHashMap` + `CopyOnWriteArrayList`）
- 参考旧版本 `RoomEntry.java` + `Namespace` 的 room 管理逻辑

**P2-4: 完善 Namespace.SocketIOClient**
- 文件: `socketio-core/.../namespace/Namespace.java`
- 问题: `sendEvent()` 和 `disconnect()` 是空方法
- 修复:
  1. `SocketIOClient` 需要持有对 `Channel` 的引用
  2. `sendEvent()` 应构建 `SocketPacket`，通过 `channel.writeAndFlush()` 发送
  3. `disconnect()` 发送 DISCONNECT packet 并关闭 channel

**P2-5: 重构 Namespace — 合并 EventRouter**
- 当前存在两套事件处理: `Namespace.on/emit` 和 `EventRouter.registerHandler/route`
- 建议统一为 `Namespace` 管理事件，废弃独立 `EventRouter`，或让 `EventRouter` 委托给 `Namespace`

**验证:** `./gradlew :socketio-core:test` 全部通过

---

### Phase 3: socketio-netty — 连接管道

**P3-1: 实现 SocketIOCodec**
- 文件: `socketio-netty/.../handler/SocketIOCodec.java`
- 入站 (`channelRead`):
  1. 接收 `EngineIOPacket<Type.MESSAGE, String>`（来自 EngineIOCodec）
  2. 调用 `SocketIODecoder.decode()` 解析为 `SocketPacket`
  3. 向下游传递 `SocketPacket`
- 出站 (`write`):
  1. 接收 `SocketPacket`
  2. 调用 `SocketIOEncoder.encode()` 编码为 String
  3. 包装为 `EngineIOPacket<Type.MESSAGE, String>` 向上游传递
  4. 若有二进制附件，需额外生成 `EngineIOPacket<Type.MESSAGE, byte[]>` 帧
- 注意处理多包情况：Socket.IO 可能在一个 MESSAGE 中携带多个数据包（`length-data` 格式）

**P3-2: 完善 SocketIONamespaceHandler**
- 文件: `socketio-netty/.../handler/SocketIONamespaceHandler.java`
- 当前已基本完成，需要:
  1. CONNECT ack 响应格式修正: `0{"/ns", "sid"}`（包含 namespace 和 session ID 数组）
  2. 集成 Room 管理（客户端连接时加入默认房间）
  3. `SocketIOClient` 创建时关联 `ChannelHandlerContext`

**P3-3: 完善 SocketIOBinaryHandler — 二进制帧处理**
- 文件: `socketio-netty/.../handler/SocketIOBinaryHandler.java`
- **Socket.IO 二进制协议（入站）：**
  1. BINARY_EVENT 的文本帧包含 JSON 占位符: `{"_placeholder":true,"num":N}`
  2. 后续依次收到 N 个 BinaryFrame（Base64 编码或原始字节）
  3. 将二进制数据收集后，替换 JSON 中的占位符为 `"base64data"`
  4. 替换完成后，解析完整的 JSON payload
- **实现思路：**
  1. 接收文本帧时，检测是否包含 `_placeholder:true`，若包含则创建 `BinaryAttachment` 开始收集
  2. 接收二进制帧时，追加到对应 session 的 `BinaryAttachment`
  3. 收集完毕后，调用 `BinaryAttachment.replacePlaceholders()` 替换占位符
  4. 将重组后的完整 payload 传递给 `SocketIODecoder` 解析
- **出站：**
  1. `SocketPacket` 若有二进制附件，拆分：先发送文本帧（含占位符 JSON），再依次发送 BinaryFrame
  2. 二进制数据通过 `BinaryWebSocketFrame` 单独发送

**P3-4: 完成 SocketIOServerPipelineFactory**
- 文件: `socketio-netty/.../pipeline/SocketIOServerPipelineFactory.java`
- 取消注释所有 Socket.IO 层 handler
- 最终 pipeline 顺序:
  ```
  HttpServerCodec
  HttpObjectAggregator
  EngineIOHandshakeHandler       — Engine.IO 握手
  EngineIOCodec                  — Engine.IO 编解码
  EngineIOHeartbeatHandler       — 心跳
  EngineIOSessionHandler         — Session 生命周期
  SocketIOCodec                  — Socket.IO 编解码
  SocketIOBinaryHandler          — 二进制附件管理
  SocketIONamespaceHandler       — Namespace/Room 管理
  SocketIOEventRouterHandler     — 事件路由
  BusinessEventHandler           — 用户回调
  GlobalExceptionHandler         — 异常处理
  ```

**P3-5: 修复 SocketIOBootstrap**
- 文件: `socketio-netty/.../bootstrap/SocketIOBootstrap.java`
- 问题: 构造函数的 `pingInterval`/`pingTimeout` 参数未被使用（第 72-73 行硬编码为 30000/25000）
- 修复: 使用传入的参数值，并从 `ServerConfig` 读取

**验证:** `./gradlew :socketio-netty:test` + 端到端测试

---

### Phase 4: socketio-server — 修复 API 和配置传递

**P4-1: 修复 SocketIOServer 端口硬编码**
- 文件: `socketio-server/.../SocketIOServer.java:25`
- 当前: `new SocketIOBootstrap(4000)` 硬编码
- 修复: 使用 `config.getOptions().getPort()`

**P4-2: 打通配置传递**
- `ServerOptions` → `SocketIOBootstrap` → `PipelineFactory`
- 需要传递的配置: `port`, `pingInterval`, `pingTimeout`, `maxFramePayloadLength`, `transports`

**P4-3: 完善 Builder**
- 当前 `Builder` 每次调用 `port()` / `pingInterval()` 都会创建新的 `ServerConfig`，丢失之前的设置
- 修复: 使用 `ServerConfig.builder()` 累积配置项

**验证:** `./gradlew :socketio-server:run` 可以启动并连接

---

### Phase 5: 集成测试

**P5-1: 基础连接测试**
- WebSocket transport 连接/断开
- 心跳 (ping/pong)
- Session 管理

**P5-2: Socket.IO 协议测试**
- CONNECT/DISCONNECT 各 namespace
- EVENT 发送/接收（含 JSON 数据）
- ACK 回调（含超时）

**P5-3: 二进制数据传输测试**
- 发送含单个 `byte[]` 的事件
- 发送含多个 `byte[]` 的事件
- 验证 `{"_placeholder":true,"num":N}` 占位符机制正确工作
- 验证二进制数据往返一致性

**P5-4: Room 管理测试**
- join/leave room
- 房间广播
- 默认房间（sessionId 命名）

**P5-5: 客户端兼容性测试**
- 使用 `io.socket:socket.io-client` 连接新服务器
- 验证协议兼容性

**验证:** 所有集成测试通过

---

### Phase 6: 废弃 netty-socketio-core

**P6-1: 功能对照**
- 对照旧模块功能清单，确认新模块已覆盖
- 缺失功能: 认证、分布式（Redisson/Hazelcast/NATS/Kafka）、SSL

**P6-2: 移除旧模块**
- 从 `settings.gradle.kts` 移除 `netty-socketio-core`
- 清理对旧模块的遗留引用

---

## 5. 关键设计决策

### 5.1 Singleton vs Dependency Injection

**当前问题:** `SessionManager`, `EventRouter`, `AckManager`, `NamespaceManager` 都是单例。

**建议:** 保持单例（短期），但通过 `ServerConfig` 集中注入配置。长期可以考虑让 manager 成为 `SocketIOBootstrap` 持有的实例，而非全局单例，以支持同一 JVM 运行多个服务器。

### 5.2 SessionManager 归属

`SessionManager` 属于 Engine.IO 层（管理 Engine.IO 会话），但 Socket.IO 层需要知道哪些 channel 属于哪些 session。通过 `ChannelAttributes.SESSION_ID` 在 Netty channel 上共享 session 信息是一个正确的做法，应继续保持。

### 5.3 事件路由架构

当前存在两套:
- `EventRouter`（全局单例，按 namespace + eventName 路由）
- `Namespace.on/emit`（按 namespace 注册 handler）

**建议:** 统一为 `Namespace` 管理事件，废弃独立 `EventRouter`。`Namespace.on()` 就是注册 handler，`SocketIONamespaceHandler` 收到事件后直接调用 `Namespace.emit()`。

### 5.4 SocketIOClient 的 Channel 引用

`Namespace.SocketIOClient` 需要能够发送数据到客户端。设计选择:

| 方案 | 优点 | 缺点 |
|------|------|------|
| 持有 `Channel` 引用 (推荐) | 可以直接 `writeAndFlush`，实现简洁 | 耦合 Netty |
| 持有 `SessionId` + `SessionManager` 查找 channel | 解耦 | 查找开销, 可能为 null |
| 注入 `Sender` 接口回调 | 完全解耦 | 实现复杂度高 |

**决定:** 方案 1 — `SocketIOClient` 持有 `Channel` 引用。作为核心库，适度耦合 Netty 是可以接受的，保持实现简洁。

### 5.5 二进制占位符机制

**采用 Socket.IO 标准格式：** `{"_placeholder":true,"num":N}`

```
发送端:
  SocketPacket { eventName: "upload", data: ["file1", <byte[] data>] }
  → JSON: ["upload", ["file1", {"_placeholder":true,"num":0}]]
  → 文本帧: "2[\"upload\",[\"file1\",{\"_placeholder\":true,\"num\":0}]]"
  → 二进制帧: <byte[] data>

接收端:
  1. 收到文本帧，解析 JSON 发现 {"_placeholder":true,"num":0}，记录需要 1 个附件
  2. 收到二进制帧，Base64 编码后替换占位符
  3. 重组 JSON: ["upload", ["file1", "base64data"]]
  4. 解析完整 payload
```

这与旧版本的 `JacksonJsonSupport.ByteArraySerializer` + `PacketDecoder.addAttachment()` 机制一致。

---

## 6. Pipeline 最终设计

```
ChannelPipeline (每个客户端连接):

  ┌─ HTTP Layer ─────────────────────────────┐
  │ HttpServerCodec                          │
  │ HttpObjectAggregator(maxFramePayloadLen) │
  └──────────────────────────────────────────┘

  ┌─ Engine.IO Layer ───────────────────────┐
  │ EngineIOHandshakeHandler                 │  握手 + Session 创建
  │ EngineIOCodec (duplex)                   │  编解码 EngineIOPacket
  │ EngineIOHeartbeatHandler (IdleState)     │  ping/pong 心跳
  │ EngineIOSessionHandler                   │  Session 生命周期
  │ [WebSocketServerProtocolHandler]         │  升级到 WS 后动态添加
  └──────────────────────────────────────────┘

  ┌─ Socket.IO Layer ───────────────────────┐
  │ SocketIOCodec (duplex)                   │  编解码 SocketPacket
  │ SocketIOBinaryHandler                    │  二进制附件收集/重组
  │ SocketIONamespaceHandler                 │  Namespace/Room/Client 管理
  │ SocketIOEventRouterHandler               │  事件路由
  └──────────────────────────────────────────┘

  ┌─ Application Layer ─────────────────────┐
  │ BusinessEventHandler                     │  用户 SocketIOListener 回调
  │ GlobalExceptionHandler                   │  异常处理
  └──────────────────────────────────────────┘
```

---

## 7. 优先级和时间线建议

| Phase | 优先级 | 预估工作量 | 依赖 | **可并行** |
|-------|--------|-----------|------|-----------|
| P0: 清理修复 | **最高** | 1-2h | 无 | — |
| P1: engineio-netty | **高** | 2-3d | P0 | P2 |
| P2: socketio-core | **高** | 2d | P0 | P1 |
| P3: socketio-netty | **中** | 1-2d | P1, P2 | — |
| P4: socketio-server | **中** | 0.5d | P3 | — |
| P5: 集成测试 | **中** | 1d | P4 | — |
| P6: 废弃旧模块 | **低** | 0.5d | P5 | — |

**并行建议:** P1 和 P2 可并行执行（两者只依赖 P0）。

---

## 8. 已知风险

1. **WebSocketServerProtocolHandler 动态添加**: 需要在握手后动态修改 pipeline，Netty 支持此操作但需要正确处理 `channelRead` 的传递
2. **Polling transport 复杂性**: 长轮询需要维护 pending response 队列，实现复杂度较高
3. **二进制数据传输**: Socket.IO 的二进制附件协议（`{"_placeholder":true,"num":N}` + out-of-band binary frames）需要精确实现，与旧版本 `PacketDecoder.addAttachment()` 逻辑对照
4. **兼容性**: 需要确保新实现的协议格式与 `socket.io-client` 完全兼容

---

## 9. 旧版本参考对照表

| 新模块类 | 旧版本参考类 | 文件路径 |
|----------|-------------|----------|
| `EngineIOHandshakeHandler` | `AuthorizeHandler` + `InPacketHandler` | `netty-socketio-core/.../handler/` |
| `EngineIOCodec` | `InPacketHandler` + `OutPacketHandler` | `netty-socketio-core/.../handler/` |
| `SocketIODecoder` | `PacketDecoder` | `netty-socketio-core/.../protocol/` |
| `SocketIOEncoder` | `PacketEncoder` | `netty-socketio-core/.../protocol/` |
| `BinaryAttachment` | `ByteArraySerializer` + `addAttachment()` | `netty-socketio-core/.../protocol/` |
| `RoomManager` | `RoomEntry` + `Namespace` room logic | `netty-socketio-core/.../namespace/` |
| `SessionManager` | `Store` (session store) | `netty-socketio-core/.../store/` |
