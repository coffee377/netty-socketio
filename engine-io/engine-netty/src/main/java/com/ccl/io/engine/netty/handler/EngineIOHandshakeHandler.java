package com.ccl.io.engine.netty.handler;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.EngineIOClient;
import com.ccl.io.engine.HandshakeData;
import com.ccl.io.engine.core.entity.ClientContext;
import com.ccl.io.engine.core.entity.OpenData;
import com.ccl.io.engine.core.session.SessionManager;

import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.EngineIOVersion;
import com.ccl.io.engine.protocol.Transport;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Engine.IO 握手协议处理器
 *
 * <p>仅处理 Engine.IO 协议握手阶段：
 * <ul>
 *   <li>验证请求路径和 transport 参数</li>
 *   <li>创建 Session 并生成 sessionId</li>
 *   <li>发送 OPEN 响应包（含连接参数）</li>
 *   <li>WebSocket 模式动态添加协议处理器</li>
 * </ul>
 * <p>
 * 握手完成后，数据传输由其他 Handler 负责。
 */
public class EngineIOHandshakeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(EngineIOHandshakeHandler.class);

    private final String connectPath;
    private final SessionManager sessionManager;
    private final int maxFramePayloadLength;

    public EngineIOHandshakeHandler(String connectPath, int maxFramePayloadLength) {
        this.connectPath = connectPath;
        this.sessionManager = SessionManager.getInstance();
        this.maxFramePayloadLength = maxFramePayloadLength;
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return super.acceptInboundMessage(msg) && isV4Handshake((FullHttpRequest) msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        handleHandshake(ctx, req);
    }

    // 辅助方法：判断是否为EngineIO握手请求
    private boolean isV4Handshake(FullHttpRequest req) {
        QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());
        String path = queryDecoder.path();

        String sid = getQueryParam(queryDecoder, "sid");
        String transport = getQueryParam(queryDecoder, "transport");
        String version = getQueryParam(queryDecoder, EngineIOVersion.EIO);
        return path.startsWith(connectPath) && HttpMethod.GET.equals(req.method()) &&
                Transport.POLLING.getName().equals(transport) &&
                sid == null && EngineIOVersion.V4.getStrValue().equals(version);
    }

    private String getQueryParam(QueryStringDecoder queryDecoder, String name) {
        List<String> values = queryDecoder.parameters().get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().findFirst().orElse(null);
    }

    private void handleHandshake(ChannelHandlerContext ctx, FullHttpRequest request) {
        Transport transportType = Transport.POLLING;

        ClientContext clientContext = sessionManager.createSession(transportType);
        EngineClient<?> client = getOrCreateClient(request, ctx.channel());
        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (clientAddress != null) {
            clientContext.setRemoteAddress(clientAddress.getAddress().toString());
        }
        ctx.channel().attr(ChannelAttributes.SESSION_ID).set(clientContext.getSessionId());
        if (log.isTraceEnabled()) {
            log.trace("Handshake {} successful for client {} with session id {}", request.uri(),
                    ctx.channel().remoteAddress(), clientContext.getSessionId());
        }

        OpenData.Builder builder = OpenData.builder(clientContext.getSessionId())
                .pingInterval(Duration.ofMillis(sessionManager.getPingInterval()))
                .pingTimeout(Duration.ofMillis(sessionManager.getPingTimeout()))
                .maxPayload(maxFramePayloadLength);
        // TODO: 2026/05/08 22:08 从配置获取可升级的传输方式
        // builder.upgrade(TransportType.WEBSOCKET);

        OpenData openData = builder.build();
        EngineIOPacket<OpenData> packet = EngineIOPacket.builder()
                .type(EngineIOPacket.Type.OPEN)
                .data(openData).build();
        ctx.writeAndFlush(packet);

    }

    private EngineClient<HandshakeData> getOrCreateClient(FullHttpRequest request, Channel channel) {
        String sid = UUID.randomUUID().toString().replace("-", "");
//        EngineClient<HandshakeData> engineClient = clients.computeIfAbsent(sid, key ->
//                EngineIOClient.builder()
//                        .sessionId(key)
//                        .engineIOVersion(4)
//                        .transport(Transport.POLLING)
//                        .connected(channel.isOpen())
////                        .handshakeData(handshakeData)
//                        .build()
//        );
//        log.debug("Create client for handshake: {}", engineClient);
//        return engineClient;
        return null;
    }

}
