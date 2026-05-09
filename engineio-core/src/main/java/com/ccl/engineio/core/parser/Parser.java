package com.ccl.engineio.core.parser;

import com.ccl.engineio.core.codec.EngineIODecoder;
import com.ccl.engineio.core.codec.EngineIOEncoder;
import com.ccl.engineio.core.codec.EngineIO;

/**
 * Engine.IO 协议解析器接口
 *
 * <p>定义 Engine.IO 协议的编解码操作，支持不同版本的协议实现</p>
 *
 * @author coffee377
 * @see <a href="https://socket.io/zh-CN/docs/v4/engine-io-protocol/">Engine.IO 协议文档</a>
 */
public interface Parser extends EngineIOEncoder, EngineIODecoder, EngineIO {

}
