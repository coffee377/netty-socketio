package com.ccl.io.engine.session;

import com.ccl.io.engine.EngineClient;
import org.jetbrains.annotations.NotNull;

public interface SessionStore {

    <T> EngineClient<T> getOrCreateClient(Class<T> handshakeClazz);

    <T> EngineClient<T> createClient(Class<T> handshakeClazz);

    boolean hasSession(@NotNull String sessionId);

    EngineClient<?> getSession(@NotNull String sessionId);

    void removeSession(@NotNull String sessionId);

    void clear();

}
