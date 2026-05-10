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
package com.socketio4j.socketio.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socketio4j.socketio.AuthorizationResult;
import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.Disconnectable;
import com.socketio4j.socketio.DisconnectableHub;
import com.socketio4j.socketio.HandshakeData;
import com.socketio4j.socketio.SocketIOClient;
import com.socketio4j.socketio.Transport;
import com.socketio4j.socketio.ack.AckManager;
import com.socketio4j.socketio.messages.HttpErrorMessage;
import com.socketio4j.socketio.namespace.Namespace;
import com.socketio4j.socketio.namespace.NamespacesHub;
import com.socketio4j.socketio.protocol.AuthPacket;
import com.socketio4j.socketio.protocol.EngineIOVersion;
import com.socketio4j.socketio.protocol.Packet;
import com.socketio4j.socketio.protocol.PacketType;
import com.socketio4j.socketio.scheduler.CancelableScheduler;
import com.socketio4j.socketio.scheduler.SchedulerKey;
import com.socketio4j.socketio.scheduler.SchedulerKey.Type;
import com.socketio4j.socketio.store.Store;
import com.socketio4j.socketio.store.StoreFactory;
import com.socketio4j.socketio.store.event.ConnectMessage;
import com.socketio4j.socketio.store.event.EventType;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 授权处理器，负责新连接的认证、会话创建和初始握手
 *
 * <p>处理 HTTP 层面的连接建立，包括路径校验、授权检查、传输方式选择、
 * 会话 ID 生成和 OPEN 数据包的发送。从 Netty pipeline 角度看，这是连接建立的入口
 */
@Sharable
public class AuthorizeHandler extends ChannelInboundHandlerAdapter implements Disconnectable {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeHandler.class);

    private final CancelableScheduler scheduler;

    private final String connectPath;
    private final Configuration configuration;
    private final NamespacesHub namespacesHub;
    private final StoreFactory storeFactory;
    private final DisconnectableHub disconnectable;
    private final AckManager ackManager;
    private final ClientsBox clientsBox;

    /**
     * 构造 AuthorizeHandler
     *
     * @param connectPath   Socket.IO 连接路径
     * @param scheduler     可取消的调度器
     * @param configuration 全局配置
     * @param namespacesHub 命名空间集线器
     * @param storeFactory  存储工厂
     * @param disconnectable 可断开组件集线器
     * @param ackManager    ACK 管理器
     * @param clientsBox    客户端容器
     */
    public AuthorizeHandler(String connectPath, CancelableScheduler scheduler, Configuration configuration, NamespacesHub namespacesHub, StoreFactory storeFactory,
            DisconnectableHub disconnectable, AckManager ackManager, ClientsBox clientsBox) {
        super();
        this.connectPath = connectPath;
        this.configuration = configuration;
        this.scheduler = scheduler;
        this.namespacesHub = namespacesHub;
        this.storeFactory = storeFactory;
        this.disconnectable = disconnectable;
        this.ackManager = ackManager;
        this.clientsBox = clientsBox;
    }

    /**
     * Channel 激活时调度首包超时检测
     *
     * <p>若客户端在 firstDataTimeout 内未发送任何数据，自动关闭连接
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel activated for client: {}", ctx.channel().remoteAddress());
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, ctx.channel());
        scheduler.schedule(key, () -> {
            log.debug("Ping timeout triggered for client: {}, closing channel", ctx.channel().remoteAddress());
            ctx.channel().close();
            log.debug("Client with ip {} opened channel but doesn't send any data! Channel closed!", ctx.channel().remoteAddress());
        }, configuration.getFirstDataTimeout(), TimeUnit.MILLISECONDS);
        super.channelActive(ctx);
    }

    /**
     * 读取 HTTP 请求，进行路径验证和连接授权
     *
     * <p>取消首包超时检测，根据路径和会话 ID 判断是新连接还是已有会话
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, ctx.channel());
        scheduler.cancel(key);

        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            Channel channel = ctx.channel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());
            
            if (log.isDebugEnabled()) {
                log.debug("Processing HTTP request: {} from client: {}", req.uri(), channel.remoteAddress());
            }

            if (!configuration.isAllowCustomRequests()
                    && !queryDecoder.path().startsWith(connectPath)) {
                if (log.isDebugEnabled()) {
                    log.debug("Rejecting invalid path request: {} from client: {}", req.uri(), channel.remoteAddress());
                }
                HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
                req.release();
                return;
            }

            List<String> sid = queryDecoder.parameters().get("sid");
            if (queryDecoder.path().equals(connectPath)
                    && sid == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Processing new connection request from client: {}", channel.remoteAddress());
                }
                String origin = req.headers().get(HttpHeaderNames.ORIGIN);
                if (!authorize(ctx, channel, origin, queryDecoder.parameters(), req)) {
                    req.release();
                    return;
                }
                // forward message to polling or websocket handler to bind channel
            } else if (sid != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Processing existing session request: {} from client: {}", sid, channel.remoteAddress());
                }
            }
        }
        ctx.fireChannelRead(msg);
    }

    /**
     * 执行连接授权逻辑
     *
     * <p>验证请求路径、传输方式，执行授权监听器，创建 ClientHead 并发送 OPEN 数据包
     *
     * @param ctx     ChannelHandlerContext
     * @param channel Channel
     * @param origin  请求来源
     * @param params  请求参数
     * @param req     HTTP 请求
     * @return 授权成功返回 true
     * @throws IOException 可能抛出的 IO 异常
     */
    private boolean authorize(ChannelHandlerContext ctx, Channel channel, String origin, Map<String, List<String>> params, FullHttpRequest req)
            throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Starting authorization for client: {} with origin: {}", channel.remoteAddress(), origin);
        }
        
        Map<String, List<String>> headers = new HashMap<String, List<String>>(req.headers().names().size());
        for (String name : req.headers().names()) {
            List<String> values = req.headers().getAll(name);
            headers.put(name, values);
        }

        HandshakeData data = new HandshakeData(req.headers(), params,
                (InetSocketAddress) channel.remoteAddress(),
                (InetSocketAddress) channel.localAddress(),
                req.uri(), origin != null && !"null".equalsIgnoreCase(origin));

        boolean result = false;
        Map<String, Object> storeParams = Collections.emptyMap();
        try {
            AuthorizationResult authResult = configuration.getAuthorizationListener().getAuthorizationResult(data);
            result = authResult.isAuthorized();
            storeParams = authResult.getStoreParams();
            if (log.isDebugEnabled()) {
                log.debug("Authorization result: {} for client: {}", result, channel.remoteAddress());
            }
        } catch (Exception e) {
            log.error("Authorization error", e);
        }

        if (!result) {
            if (log.isDebugEnabled()) {
                log.debug("Authorization failed for client: {}, sending UNAUTHORIZED response", channel.remoteAddress());
            }
            HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
            channel.writeAndFlush(res)
                    .addListener(ChannelFutureListener.CLOSE);
            log.debug("Handshake unauthorized, query params: {} headers: {}", params, headers);
            return false;
        }

        UUID sessionId;
        if (configuration.isRandomSession()) {
            sessionId = UUID.randomUUID();
            if (log.isDebugEnabled()) {
                log.debug("Generated random session ID: {} for client: {}", sessionId, channel.remoteAddress());
            }
        } else {
            sessionId = this.generateOrGetSessionIdFromRequest(req.headers());
            if (log.isDebugEnabled()) {
                log.debug("Retrieved existing session ID: {} for client: {}", sessionId, channel.remoteAddress());
            }
        }

        List<String> transportValue = params.get("transport");
        if (transportValue == null) {
            if (log.isDebugEnabled()) {
                log.debug("Missing transport parameter for client: {}, sending transport error", channel.remoteAddress());
            }
            log.error("Got no transports for request {}", req.uri());
            writeAndFlushTransportError(channel, origin);
            return false;
        }

        Transport transport;
        try {
            transport = Transport.valueOf(transportValue.get(0).toUpperCase());
            if (log.isDebugEnabled()) {
                log.debug("Transport resolved: {} for client: {}", transport, channel.remoteAddress());
            }
        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid transport value: {} for client: {}", transportValue.get(0), channel.remoteAddress());
            }
            log.error("Unknown transport for request {}", req.uri());
            writeAndFlushTransportError(channel, origin);
            return false;
        }
        if (!configuration.getTransports().contains(transport)) {
            if (log.isDebugEnabled()) {
                log.debug("Unsupported transport: {} for client: {}, sending transport error", transport, channel.remoteAddress());
            }
            log.error("Unsupported transport for request {}", req.uri());
            writeAndFlushTransportError(channel, origin);
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Creating client head for session: {} with transport: {} for client: {}", sessionId, transport, channel.remoteAddress());
        }
        
        ClientHead client = new ClientHead(sessionId, ackManager, disconnectable, storeFactory, data, clientsBox, transport, scheduler, configuration, params);
        Store store = client.getStore();
        storeParams.forEach(store::set);
        channel.attr(ClientHead.CLIENT).set(client);
        clientsBox.addClient(client);

        String[] transports = {};
        //:TODO lyjnew   Current WEBSOCKET retrun upgrade[] engine-io protocol
        // the test case line
        // https://github.com/socketio/engine.io-protocol/blob/de247df875ddcd4778d1165829c8644301750e9f/test-suite/test-suite.js#L131C43-L131C43
        if (configuration.getTransports().contains(Transport.WEBSOCKET)
                && !(EngineIOVersion.V4.equals(client.getEngineIOVersion()) && Transport.WEBSOCKET.equals(client.getCurrentTransport())))  {
            transports = new String[]{"websocket"};
            if (log.isDebugEnabled()) {
                log.debug("WebSocket upgrade available for client: {}", channel.remoteAddress());
            }
        }

        AuthPacket authPacket = new AuthPacket(sessionId, transports, configuration.getPingInterval(),
                configuration.getPingTimeout());
        Packet packet = new Packet(PacketType.OPEN, client.getEngineIOVersion());
        packet.setData(authPacket);
        
        if (log.isDebugEnabled()) {
            log.debug("Sending OPEN packet to client: {} with session: {}", channel.remoteAddress(), sessionId);
        }
        
        client.send(packet);

        client.schedulePing();
        client.schedulePingTimeout();
        log.debug("Handshake authorized for sessionId: {}, query params: {} headers: {}", sessionId, params, headers);
        return true;
    }

    /**
     * 写入并刷新传输错误响应
     *
     * @param channel Channel
     * @param origin  请求来源
     */
    private void writeAndFlushTransportError(Channel channel, String origin) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("code", 0);
        errorData.put("message", "Transport unknown");

        channel.attr(EncoderHandler.ORIGIN).set(origin);
        channel.writeAndFlush(new HttpErrorMessage(errorData));
    }

    /**
     * 从请求中获取或生成会话 ID
     *
     * <p>优先从 "io" 请求头或 Cookie 中读取，解析失败时生成随机 UUID
     *
     * @param headers HTTP 请求头
     * @return 会话 UUID
     */
    private UUID generateOrGetSessionIdFromRequest(HttpHeaders headers) {
        List<String> values = headers.getAll("io");
        if (values.size() == 1) {
            try {
                return UUID.fromString(values.get(0));
            } catch (IllegalArgumentException iaex) {
                log.warn("Malformed UUID received for session! io={}", values.get(0));
            }
        }

        for (String cookieHeader : headers.getAll(HttpHeaderNames.COOKIE)) {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieHeader);

            for (Cookie cookie : cookies) {
                if (cookie.name().equals("io")) {
                    try {
                        return UUID.fromString(cookie.value());
                    } catch (IllegalArgumentException iaex) {
                        log.warn("Malformed UUID received for session! io={}", cookie.value());
                    }
                }
            }
        }

        return UUID.randomUUID();
    }

    /**
     * 根据会话 ID 连接客户端
     *
     * @param sessionId 会话 ID
     */
    public void connect(UUID sessionId) {
        if (log.isDebugEnabled()) {
            log.debug("Connecting client with session ID: {}", sessionId);
        }
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, sessionId);
        scheduler.cancel(key);
    }

    /**
     * 将客户端连接到默认命名空间
     *
     * <p>发送 CONNECT 数据包，发布连接事件，触发命名空间的 onConnect 回调
     *
     * @param client 客户端头部
     */
    public void connect(ClientHead client) {
        if (log.isDebugEnabled()) {
            log.debug("Connecting client: {} to default namespace", client.getSessionId());
        }
        
        Namespace ns = namespacesHub.get(Namespace.DEFAULT_NAME);

        if (!client.getNamespaces().contains(ns)) {
            Packet packet = new Packet(PacketType.MESSAGE, client.getEngineIOVersion());
            packet.setSubType(PacketType.CONNECT);
            //::TODO lyjnew V4 delay send connect packet  ON client add Namecapse
            if (!EngineIOVersion.V4.equals(client.getEngineIOVersion())) {
                if (log.isDebugEnabled()) {
                    log.debug("Sending CONNECT packet to client: {}", client.getSessionId());
                }
                client.send(packet);
            }

            configuration.getStoreFactory().eventStore().publish(EventType.CONNECT, new ConnectMessage(client.getSessionId()));

            SocketIOClient nsClient = client.addNamespaceClient(ns);
            ns.onConnect(nsClient);
            
            if (log.isDebugEnabled()) {
                log.debug("Client: {} successfully connected to default namespace", client.getSessionId());
            }
        }
    }

    /**
     * 客户端断开时将其从 ClientsBox 中移除
     *
     * @param client 断开的客户端
     */
    @Override
    public void onDisconnect(ClientHead client) {
        if (log.isDebugEnabled()) {
            log.debug("Client disconnected: {}", client.getSessionId());
        }
        clientsBox.removeClient(client.getSessionId());
    }

}
