package com.ccl.io.engine.netty.handler;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.EngineIOClient;
import com.ccl.io.engine.Handshake;
import com.ccl.io.engine.auth.AuthResult;
import com.ccl.io.engine.auth.Authenticator;
import com.ccl.io.engine.core.entity.OpenData;

import com.ccl.io.engine.core.store.MemoryEngineClientStore;
import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.EngineIOVersion;
import com.ccl.io.engine.protocol.Transport;
import com.ccl.io.engine.store.EngineClientStore;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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
    private final int maxFramePayloadLength;
    private final Authenticator authenticator;
    private final EngineClientStore<EngineIOClient.Builder> store;
    private QueryStringDecoder queryDecoder;

    public EngineIOHandshakeHandler(String connectPath, int maxFramePayloadLength) {
        this.connectPath = connectPath;
        this.maxFramePayloadLength = maxFramePayloadLength;
        this.authenticator = Authenticator.NOOP;
        this.store = new MemoryEngineClientStore();
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return super.acceptInboundMessage(msg) && isHandshake((FullHttpRequest) msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        Channel channel = ctx.channel();
        if (log.isDebugEnabled()) {
            log.debug("Processing HTTP request: {} from client: {}", req.uri(), channel.remoteAddress());
        }

        Map<String, List<String>> headers = new HashMap<>(req.headers().names().size());
        for (String name : req.headers().names()) {
            List<String> values = req.headers().getAll(name);
            headers.put(name, values);
        }

        Map<String, List<String>> params = queryDecoder.parameters();
        // 判断跨域
        boolean cross = isCrossOrigin(req, "http", "localhost", 4000);
        Handshake handshake = Handshake.builder().url(req.uri())
                .urlParams(params)
                .localAddress((InetSocketAddress) channel.localAddress())
                .remoteAddress((InetSocketAddress) channel.remoteAddress())
                .xDomain(cross)
                .build();

        AuthResult authResult = authenticator.authenticate(handshake);

        if (!authResult.isAuthorized()) {
            if (log.isDebugEnabled()) {
                log.debug("Authorization failed for client: {}, sending UNAUTHORIZED response", channel.remoteAddress());
            }
            HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
            channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
            log.debug("Handshake unauthorized, query params: {} headers: {}", params, headers);
            return;
        }

        String origin = req.headers().get(HttpHeaderNames.ORIGIN);
        Transport transport = getTransport();
        Integer version = Integer.valueOf(Optional.ofNullable(getQueryParam(queryDecoder, EngineIOVersion.EIO)).orElse("4"));
        EngineIOClient.Builder builder = EngineIOClient.builder()
                .sessionId(UUID.randomUUID().toString().replace("-", ""))
                .engineIOVersion(version)
                .transport(transport)
                .handshakeData(handshake);

        EngineClient client = store.createClient(builder);
        channel.attr(ChannelAttributes.ENGINE_CLIENT).set(client);
        channel.attr(ChannelAttributes.SESSION_ID).set(client.getSessionId());

        OpenData.Builder openBuilder = OpenData.builder(client.getSessionId())
                .pingInterval(Duration.ofMillis(30_000))
                .pingTimeout(Duration.ofMillis(25_000))
                .maxPayload(maxFramePayloadLength);
        // TODO: 2026/05/08 22:08 从配置获取可升级的传输方式
        // builder.upgrade(TransportType.WEBSOCKET);

        OpenData openData = openBuilder.build();
        EngineIOPacket<OpenData> packet = EngineIOPacket.builder()
                .type(EngineIOPacket.Type.OPEN)
                .data(openData).build();

        // client.send(packet);

        ctx.writeAndFlush(packet);
    }

    public static boolean isCrossOrigin(FullHttpRequest request, String myScheme, String myHost, int myPort) {
        HttpHeaders headers = request.headers();
        String origin = headers.get(HttpHeaderNames.ORIGIN);
        if (origin == null || origin.isEmpty()) {
            return false; // 无 Origin → 非跨域
        }

        // 构造本服务的源：scheme://host:port
        String myOrigin = String.format("%s://%s:%d", myScheme, myHost, myPort);

        // origin 不一致 → 跨域
        return !origin.equalsIgnoreCase(myOrigin);
    }

    private Transport getTransport() {
        String transport = getQueryParam(queryDecoder, "transport");
        return Transport.of(transport);
    }

    // 辅助方法：判断是否为EngineIO握手请求
    private boolean isHandshake(FullHttpRequest req) {
        queryDecoder = new QueryStringDecoder(req.uri());
        String sid = getQueryParam(queryDecoder, "sid");
        return queryDecoder.path().startsWith(connectPath)
                && HttpMethod.GET.equals(req.method()) && sid == null;
    }

    private String getQueryParam(QueryStringDecoder queryDecoder, String name) {
        List<String> values = queryDecoder.parameters().get(name);
        if (values == null || values.isEmpty()) return null;
        return values.stream().findFirst().orElse(null);
    }

}
