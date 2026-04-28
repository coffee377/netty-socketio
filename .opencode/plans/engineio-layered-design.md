# Engine.IO 分层重构设计

**日期**: 2026-04-28  
**状态**: 草稿  
**目标**: 将 Engine.IO 模块重构为层次清晰、协议驱动的分层架构

---

## 1. 背景与目标

### 1.1 现状问题
- `netty-socketio-core` 原模块 Netty 处理逻辑层次不够清晰
- 部分组件职责混杂（编解码、处理器、handler 边界模糊）
- 存在废弃代码与新代码共存

### 1.2 重构目标
1. **Protocol 层分离**: Engine.IO 和 Socket.IO 协议完全独立
2. **Codec 解耦**: 编解码与 Handler 完全分离
3. **Handler 职责单一**: 每个 Handler 只处理一种职责
4. **协议验证**: 实现过程中持续验证协议符合性

### 1.3 废弃清单
| 废弃组件 | 替代方案 |
|----------|----------|
| `EnginePacketType` | `EngineIOPacket.Type` |
| `EngineIOEncoder` / `EngineIODecoder` | `ParserV4` |
| `EnginePacket` | `EngineIOPacket` |
| `SessionManager` (单例) | `SessionManager` 接口 + `InMemorySessionManager` |

---

## 2. 架构设计

### 2.1 模块结构

```
engineio-common/     # 枚举、常量、工具类
    └── enums/EnginePacketType (废弃)
    └── enums/TransportType
    └── constant/ProtocolConstants
    └── exception/
    └── util/

engineio-core/       # 协议核心层（无 Netty 依赖）
    └── protocol/EngineIOPacket      # 协议数据包
    └── parser/ParserV4              # v4 协议解析
    └── session/SessionManager       # 接口定义
    └── session/ClientContext       # 客户端上下文
    └── processor/                   # 业务处理器

engineio-netty/      # Netty 实现层
    └── pipeline/PipelineFactory
    └── handler/
        ├── EngineIOHandshakeHandler
        ├── EngineIOCodec           # Duplex: ByteBuf ↔ EngineIOPacket
        ├── EngineIOSessionHandler
        ├── EngineIOHeartbeatHandler
        └── WebSocketUpgradeHandler
```

### 2.2 Pipeline 结构

```
HttpServerCodec
HttpObjectAggregator
       ↓
EngineIOHandshakeHandler    [Layer 3: 握手]
       ↓
EngineIOCodec                [Layer 2: 编解码 - Duplex]
       ↓
EngineIOSessionHandler       [Layer 4: 会话绑定]
       ↓
EngineIOHeartbeatHandler     [Layer 5: 心跳]
       ↓
WebSocketUpgradeHandler      [Layer 6: 传输升级]
       ↓
[后续 Socket.IO 层]
```

### 2.3 数据流

```
入站 (Inbound):
  ByteBuf → EngineIOCodec → EngineIOPacket → EngineIOSessionHandler → ...

出站 (Outbound):
  EngineIOPacket → EngineIOCodec → ByteBuf → ...
```

---

## 3. 分层实现计划

### Layer 1: Session 接口层

**目标**: 定义 Session 管理层抽象，支持多种存储实现

#### 3.1.1 SessionManager 接口

```java
public interface SessionManager {
    String createSession();
    ClientContext getSession(String sessionId);
    boolean hasSession(String sessionId);
    void removeSession(String sessionId);
    void scheduleSessionTimeout(String sessionId, Runnable onTimeout);
    void cancelSessionTimeout(String sessionId);
    void updatePingTime(String sessionId);
    Collection<ClientContext> getAllSessions();
}
```

#### 3.1.2 InMemorySessionManager 实现

```java
public class InMemorySessionManager implements SessionManager {
    private final ConcurrentHashMap<String, ClientContext> sessions;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> timeoutTasks;
}
```

**验证测试**:
```java
@Test
void shouldCreateAndRetrieveSession() {
    SessionManager manager = new InMemorySessionManager();
    String sid = manager.createSession();
    assertNotNull(manager.getSession(sid));
    assertTrue(manager.hasSession(sid));
}

@Test
void shouldRemoveSession() {
    String sid = manager.createSession();
    manager.removeSession(sid);
    assertFalse(manager.hasSession(sid));
}
```

---

### Layer 2: Codec 层

**目标**: 实现 Netty ByteBuf ↔ EngineIOPacket 双向转换

#### 3.2.1 EngineIOCodec

```java
@ChannelHandler.Sharable
public class EngineIOCodec extends ChannelDuplexHandler {
    
    private final Parser parser;
    private final boolean supportsBinary;
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf buf) {
            EngineIOPacket<?> packet = decode(buf);
            ctx.fireChannelRead(packet);
        }
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof EngineIOPacket<?> packet) {
            ByteBuf buf = encode(packet);
            super.write(ctx, buf, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
```

**协议约束验证**:
- 文本帧: `4` + payload
- 二进制帧: `b` + base64(payload)
- Payload (HTTP Polling): 使用 0x1E 分隔符

**验证测试**:
```java
@Test
void shouldEncodeMessagePacket() {
    EngineIOPacket<String> packet = EngineIOPacket.of("hello");
    ByteBuf buf = codec.encode(packet);
    assertEquals("4hello", buf.toString(UTF_8));
}

@Test
void shouldDecodeMessagePacket() {
    ByteBuf buf = buffer("4hello");
    EngineIOPacket<?> packet = codec.decode(buf);
    assertEquals(MESSAGE, packet.getType());
}

@Test
void shouldHandlePayloadWithMultiplePackets() {
    ByteBuf buf = buffer("4hello\u001E2\u001E4world");
    List<EngineIOPacket<?>> packets = codec.decodePayload(buf);
    assertEquals(3, packets.size());
}
```

---

### Layer 3: Handshake 层

**目标**: 完成 Engine.IO 握手流程

#### 3.3.1 HandshakeProcessor

```java
public class HandshakeProcessor {
    
    public Map<String, Object> createOpenPayload(String sid, List<String> transports) {
        return Map.of(
            "sid", sid,
            "upgrades", transports,
            "pingInterval", pingInterval,
            "pingTimeout", pingTimeout,
            "maxPayload", maxPayload
        );
    }
    
    public boolean isValidHandshake(String eioVersion, String transport) {
        return "4".equals(eioVersion) && isValidTransport(transport);
    }
}
```

#### 3.3.2 EngineIOHandshakeHandler

```java
public class EngineIOHandshakeHandler extends ChannelInboundHandlerAdapter {
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest req) {
            handleHandshake(ctx, req);
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
```

**握手流程**:
```
Client: GET /?EIO=4&transport=polling
Server: 200 OK "0{"sid":"abc123","pingInterval":25000,"pingTimeout":20000,...}"
```

**验证测试**:
```java
@Test
void shouldReturnOpenPacketOnValidHandshake() {
    // 发送 ?EIO=4&transport=polling
    // 验证响应: 200 OK + payload starts with "0{"
}
```

---

### Layer 4: Session Handler 层

**目标**: Channel 绑定 Session 生命周期

#### 3.4.1 ChannelAttributes

```java
public class ChannelAttributes {
    public static final AttributeKey<String> SESSION_ID = ...;
    public static final AttributeKey<ClientContext> CLIENT_CONTEXT = ...;
    public static final AttributeKey<TransportType> TRANSPORT_TYPE = ...;
}
```

#### 3.4.2 EngineIOSessionHandler

```java
public class EngineIOSessionHandler extends ChannelInboundHandlerAdapter {
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ClientContext context = getSessionContext(ctx);
        ctx.fireChannelRead(new EnginePacketEnvelope(msg, context));
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String sid = ctx.channel().attr(SESSION_ID).get();
        if (sid != null) {
            sessionManager.removeSession(sid);
        }
    }
}
```

**验证测试**:
```java
@Test
void shouldBindSessionToChannel() {
    // 握手后
    assertNotNull(channel.attr(SESSION_ID).get());
}
```

---

### Layer 5: Heartbeat 层

**目标**: Ping/Pong 心跳机制

#### 3.5.1 HeartbeatProcessor

```java
public class HeartbeatProcessor {
    public EngineIOPacket<String> createPing() {
        return EngineIOPacket.of(Type.PING);
    }
    
    public EngineIOPacket<String> createPong() {
        return EngineIOPacket.of(Type.PONG);
    }
}
```

#### 3.5.2 EngineIOHeartbeatHandler

```java
public class EngineIOHeartbeatHandler extends IdleStateHandler {
    
    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        if (evt.state() == READER_IDLE) {
            // 超时断开
            sessionManager.removeSession(sid);
            ctx.close();
        } else if (evt.state() == WRITER_IDLE) {
            // 发送 Ping
            ctx.writeAndFlush(heartbeatProcessor.createPing());
        }
    }
}
```

**协议约束**:
- 服务端定期发送 PING (pingInterval)
- 客户端应在 pingTimeout 内响应 PONG
- 超时断开连接

**验证测试**:
```java
@Test
void shouldSendPingOnWriterIdle() {
    // 验证写空闲时发送 PING 包
}

@Test
void shouldCloseOnReaderIdleTimeout() {
    // 验证超时后连接关闭
}
```

---

### Layer 6: WebSocket Upgrade 层

**目标**: Polling → WebSocket 平滑升级

#### 3.6.1 WebSocketUpgradeHandler

```java
public class WebSocketUpgradeHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            // 1. 标记传输升级
            sessionManager.upgradeTransport(sid, TransportType.WEBSOCKET);
            // 2. 发送 UPGRADE 包
            ctx.writeAndFlush(EngineIOPacket.of(Type.UPGRADE));
            // 3. 关闭 polling 连接
        }
    }
}
```

**协议约束**:
- UPGRADE 包 (5) 通知客户端可以切换到 WebSocket
- NOOP 包 (6) 用于清理 polling

**验证测试**:
```java
@Test
void shouldSendUpgradePacketOnWebSocketConnect() {
    // WebSocket 握手完成后验证发送 UPGRADE
}
```

---

### Layer 7: Pipeline 集成

**目标**: 组装完整 Pipeline

```java
public class EngineIOPipelineFactory extends ChannelInitializer<Channel> {
    
    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        
        // HTTP
        pipeline.addLast("httpCodec", new HttpServerCodec());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));
        
        // Engine.IO Layers
        pipeline.addLast("handshake", new EngineIOHandshakeHandler(handshakeProcessor));
        pipeline.addLast("codec", new EngineIOCodec(parser, supportsBinary));
        pipeline.addLast("session", new EngineIOSessionHandler(sessionManager));
        pipeline.addLast("heartbeat", new EngineIOHeartbeatHandler(heartbeatProcessor, sessionManager));
        pipeline.addLast("upgrade", new WebSocketUpgradeHandler(upgradeProcessor));
    }
}
```

---

### Layer 8: 协议一致性验证

**目标**: 确保所有 packet type 符合官方协议

| Packet | Type | 触发条件 | 验证点 |
|--------|------|----------|--------|
| OPEN | 0 | 握手响应 | payload 包含 sid, pingInterval, pingTimeout |
| CLOSE | 1 | 连接关闭 | 收到后正确释放资源 |
| PING | 2 | 定时触发 | 写空闲时发送 |
| PONG | 3 | 响应 PING | 收到后重置超时计时器 |
| MESSAGE | 4 | 业务数据 | 正确编解码 |
| UPGRADE | 5 | WebSocket 连接 | 升级时发送一次 |
| NOOP | 6 | 升级清理 | polling 清理 |

---

## 4. 异常处理策略

**原则**: Handler 内处理

```java
public class EngineIOCodec extends ChannelDuplexHandler {
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Engine.IO codec error", cause);
        ctx.close();
    }
}
```

---

## 5. 测试策略

### 5.1 单元测试
- `ParserV4Test`: 已有，验证协议编解码
- `SessionManagerTest`: 验证会话管理
- 各 Handler 独立测试

### 5.2 集成测试
- 完整握手流程测试
- 心跳机制测试
- WebSocket 升级测试

### 5.3 协议一致性测试
- 对照官方协议文档验证每个 packet type

---

## 6. 实现顺序

```
Layer 1: Session 接口层
  └── 定义接口 + 内存实现 + 基础测试

Layer 2: Codec 层  
  └── 重写 EngineIOCodec + 协议验证测试

Layer 3: Handshake + Session Handler
  └── 握手流程 + 会话绑定

Layer 4: Heartbeat + Upgrade
  └── 心跳机制 + WebSocket 升级

Layer 5: Pipeline 集成 + 协议验证
  └── 组装 pipeline + 完整测试
```

---

## 7. 参考文档

- [Engine.IO Protocol v4](https://socket.io/zh-CN/docs/v4/engine-io-protocol/)
- [Socket.IO Protocol](https://socket.io/zh-CN/docs/v4/socket-io-protocol/)
