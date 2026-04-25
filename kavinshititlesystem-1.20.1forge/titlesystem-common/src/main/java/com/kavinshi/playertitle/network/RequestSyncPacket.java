package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * 请求同步数据包，用于客户端向服务器请求同步标题数据。
 * 可以请求完整同步或增量同步。
 */
@SuppressWarnings("null")
public class RequestSyncPacket extends AbstractPacket {

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
