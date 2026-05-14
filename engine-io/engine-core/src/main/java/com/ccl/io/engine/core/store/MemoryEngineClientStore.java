package com.ccl.io.engine.core.store;

import com.ccl.io.engine.EngineClient;
import com.ccl.io.engine.EngineIOClient;
import com.ccl.io.engine.store.EngineClientStore;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryEngineClientStore implements EngineClientStore<EngineIOClient.Builder> {

    private final Map<String, EngineClient> clients = new ConcurrentHashMap<>();

    @Override
    public EngineClient createClient(EngineIOClient.Builder builder) {
        EngineClient client = builder.build();
        return clients.computeIfAbsent(client.getSessionId(), key-> client);
    }

    @Override
    public boolean hasClient(@NotNull String sessionId) {
        return clients.containsKey(sessionId);
    }

    @Override
    public EngineClient getClient(@NotNull String sessionId) {
        return clients.get(sessionId);
    }

    @Override
    public void removeClient(@NotNull String sessionId) {
        clients.remove(sessionId);
    }
}
