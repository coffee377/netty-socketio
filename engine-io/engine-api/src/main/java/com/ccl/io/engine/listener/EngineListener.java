package com.ccl.io.engine.listener;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.protocol.EngineIOPacket;
import com.ccl.io.engine.protocol.Transport;

public interface EngineListener {

    void onPacket(EngineIOPacket<?> packet, EngineClient client, Transport transport);

}
