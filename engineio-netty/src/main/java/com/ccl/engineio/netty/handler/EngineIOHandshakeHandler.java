package com.ccl.engineio.netty.handler;

import com.ccl.engineio.core.entity.ClientContext;
import com.ccl.engineio.core.entity.OpenData;
import com.ccl.engineio.core.parser.ParserV4;
import com.ccl.engineio.core.protocol.EngineIOPacket;
import com.ccl.engineio.core.protocol.EngineVersion;
import com.ccl.engineio.core.protocol.TransportType;
import com.ccl.engineio.core.session.SessionManager;

import io.netty.channel.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

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
        if (log.isDebugEnabled()) {
            log.debug("Handshake: {}, {}", ctx.channel().remoteAddress(), req.uri());
        }
        handleHandshake(ctx, req);
    }

    // 辅助方法：判断是否为EngineIO握手请求
    private boolean isV4Handshake(FullHttpRequest req) {
        QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());
        String path = queryDecoder.path();

        String sid = getQueryParam(queryDecoder, "sid");
        String transport = getQueryParam(queryDecoder, "transport");
        String version = getQueryParam(queryDecoder, EngineVersion.EIO);
        return path.startsWith(connectPath) && HttpMethod.GET.equals(req.method()) &&
                TransportType.POLLING.getName().equals(transport) &&
                sid == null && EngineVersion.V4.getStrValue().equals(version);
    }

    private String getQueryParam(QueryStringDecoder queryDecoder, String name) {
        List<String> values = queryDecoder.parameters().get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().findFirst().orElse(null);
    }

    private void handleHandshake(ChannelHandlerContext ctx, FullHttpRequest request) {
        TransportType transportType = TransportType.POLLING;

        ClientContext clientContext = sessionManager.createSession(transportType);
        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (clientAddress != null) {
            clientContext.setRemoteAddress(clientAddress.getAddress().toString());
        }
        ctx.channel().attr(ChannelAttributes.SESSION_ID).set(clientContext.getSessionId());
        if (log.isDebugEnabled()) {
            log.debug("Handshake successful for client: {} with session: {}",
                    ctx.channel().remoteAddress(), clientContext.getSessionId());
        }

        OpenData.Builder builder = OpenData.builder(clientContext.getSessionId())
                .pingInterval(Duration.ofMillis(sessionManager.getPingInterval()))
                .pingTimeout(Duration.ofMillis(sessionManager.getPingTimeout()))
                .maxPayload(maxFramePayloadLength);
        // builder.upgrade(TransportType.WEBSOCKET);

        OpenData openData = builder.build();
        EngineIOPacket<OpenData> packet = EngineIOPacket.builder().type(EngineIOPacket.Type.OPEN).data(openData).build();
        ctx.channel().writeAndFlush(packet);

    }

}
