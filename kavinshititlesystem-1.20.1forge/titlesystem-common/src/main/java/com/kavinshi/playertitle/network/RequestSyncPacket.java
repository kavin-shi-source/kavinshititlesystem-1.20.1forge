package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class RequestSyncPacket extends AbstractPacket {
    private static final Logger LOGGER = LogManager.getLogger(RequestSyncPacket.class);

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
            PacketHandlers.handleRequestSync(sender, playerId, fullSync);
        }
    }

    public UUID getPlayerId() { return playerId; }
    public boolean isFullSync() { return fullSync; }
}
