package com.ccl.io.engine.store;

import com.ccl.io.engine.EngineClient;
import org.jetbrains.annotations.NotNull;

public interface EngineClientStore<B> {

    boolean hasClient(@NotNull String sessionId);

    EngineClient getClient(@NotNull String sessionId);

    EngineClient createClient(B builder);

    void removeClient(@NotNull String sessionId);

}
