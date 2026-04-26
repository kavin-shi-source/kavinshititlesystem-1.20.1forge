package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class RequestSyncPacket extends AbstractPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestSyncPacket.class);
    private static final long COOLDOWN_MS = 5000L;
    private static final ConcurrentHashMap<UUID, Long> lastRequestTime = new ConcurrentHashMap<>();

    private final UUID playerId;
    private final boolean fullSync;

    public RequestSyncPacket(UUID playerId, boolean fullSync) {
        this.playerId = playerId;
        this.fullSync = fullSync;
    }

    public RequestSyncPacket(FriendlyByteBuf buffer) {
        this.playerId = buffer.readUUID();
        this.fullSync = buffer.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeBoolean(fullSync);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            UUID senderId = sender.getUUID();
            long now = System.currentTimeMillis();
            if (lastRequestTime.compute(senderId, (k, lastTime) -> {
                if (lastTime != null && now - lastTime < COOLDOWN_MS) return lastTime;
                return now;
            }) != now) {
                LOGGER.debug("Rate-limited sync request from {}", sender.getGameProfile().getName());
                return;
            }
            PacketHandlers.handleRequestSync(sender, senderId, fullSync);
        }
    }

    public static void cleanupPlayer(UUID playerId) {
        lastRequestTime.remove(playerId);
    }

    public UUID getPlayerId() { return playerId; }
    public boolean isFullSync() { return fullSync; }
}
