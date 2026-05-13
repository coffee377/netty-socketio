package com.ccl.io.engine.message;

import com.ccl.io.engine.core.entity.ClientContext;
import com.ccl.io.engine.protocol.Transport;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

/**
 * Engine.IO 传输消息封装
 *
 * <p>包含客户端上下文、消息内容和使用的传输方式。
 * 实现了 {@link ReferenceCounted} 接口以支持 Netty 引用计数生命周期管理。
 * </p>
 *
 * @author coffee377
 * @since 4.0.0
 */
public class EngineMessage implements ReferenceCounted {

    private final ClientContext client;
    private final ByteBuf content;
    private final Transport transport;

    /**
     * 获取客户端上下文
     *
     * @return 客户端上下文信息
     */
    public ClientContext getClient() {
        return client;
    }

    /**
     * 获取消息内容
     *
     * @return 消息内容的 ByteBuf
     */
    public ByteBuf getContent() {
        return content;
    }

    /**
     * 获取传输方式
     *
     * @return 传输方式（POLLING 或 WEBSOCKET）
     */
    public Transport getTransport() {
        return transport;
    }

    /**
     * 创建消息构建器
     *
     * @return 新的 Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    private EngineMessage(Builder builder) {
        this.client = builder.client;
        this.content = builder.content;
        this.transport = builder.transport;
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        content.retain();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        content.retain(increment);
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        content.touch();
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        content.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }

    /**
     * EngineMessage 构建器
     *
     * <p>使用建造者模式创建 EngineMessage 实例，默认传输方式为 POLLING。
     * </p>
     */
    public static class Builder {
        private ClientContext client;
        private ByteBuf content;
        private Transport transport;

        /**
         * 设置客户端上下文
         *
         * @param client 客户端上下文
         * @return 当前构建器实例
         */
        public Builder client(ClientContext client) {
            this.client = client;
            return this;
        }

        /**
         * 设置消息内容
         *
         * @param content 消息内容的 ByteBuf
         * @return 当前构建器实例
         */
        public Builder content(ByteBuf content) {
            this.content = content;
            return this;
        }

        /**
         * 设置传输方式
         *
         * @param transport 传输方式
         * @return 当前构建器实例
         */
        public Builder transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * 构建消息实例
         *
         * @return 完整的 EngineMessage 实例
         */
        public EngineMessage build() {
            if (transport == null) {
                transport = Transport.POLLING;
            }
            return new EngineMessage(this);
        }
    }
}
