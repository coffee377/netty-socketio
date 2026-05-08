package com.ccl.socketio.core.codec;

import com.ccl.socketio.core.protocol.SocketPacket;

public interface SocketEncoder {

    String encode(SocketPacket<?> packet);

}
