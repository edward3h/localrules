package org.ethelred.minecraft;

import io.micronaut.core.annotation.Nullable;

public record MinecraftServerEvent(
        Type type,
        String containerId,
        String containerName,
        String worldName,
        @Nullable String playerName,
        @Nullable String playerXuid
) {
    public enum Type {SERVER_STARTED, SERVER_STOPPED, PLAYER_CONNECTED, PLAYER_DISCONNECTED}
}
