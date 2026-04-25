package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * 标题更新网络数据包，用于在客户端和服务器之间同步标题状态变化。
 * 支持多种更新类型：标题解锁、装备、撤销、击杀数更新和存活时间更新。
 */
@SuppressWarnings("null")
public class TitleUpdatePacket extends AbstractPacket {

    public enum UpdateType {
        TITLE_UNLOCKED(0), TITLE_EQUIPPED(1), TITLE_REVOKED(2), KILL_COUNT_UPDATED(3), ALIVE_TIME_UPDATED(4);
        private final int id;
        UpdateType(int id) { this.id = id; }
        public int getId() { return id; }
        public static UpdateType fromId(int id) {
            for (UpdateType type : values()) if (type.id == id) return type;
            return TITLE_UNLOCKED;
        }
    }

    private final UUID playerId;
    private final UpdateType updateType;
    private final int titleId;
    private final String entityId;
    private final int count;

    public TitleUpdatePacket(UUID playerId, UpdateType updateType, int titleId) {
        this(playerId, updateType, titleId, "", 0);
    }

    public TitleUpdatePacket(UUID playerId, String entityId, int count) {
        this(playerId, UpdateType.KILL_COUNT_UPDATED, -1, entityId, count);
    }

    public TitleUpdatePacket(UUID playerId, int aliveMinutes) {
        this(playerId, UpdateType.ALIVE_TIME_UPDATED, -1, "", aliveMinutes);
    }

    private TitleUpdatePacket(UUID playerId, UpdateType updateType, int titleId, String entityId, int count) {
        this.playerId = playerId;
        this.updateType = updateType;
        this.titleId = titleId;
        this.entityId = entityId != null ? entityId : "";
        this.count = count;
    }

    public TitleUpdatePacket(FriendlyByteBuf buffer) {
        this.playerId = buffer.readUUID();
        this.updateType = UpdateType.fromId(buffer.readVarInt());
        this.titleId = buffer.readVarInt();
        this.entityId = readString(buffer);
        this.count = buffer.readVarInt();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeVarInt(updateType.getId());
        buffer.writeVarInt(titleId);
        writeString(buffer, entityId);
        buffer.writeVarInt(count);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            PacketHandlers.handleTitleUpdateServer(sender, updateType, playerId, titleId);
        } else {
            PacketHandlers.handleTitleUpdateClient(updateType, playerId, titleId, entityId, count);
        }
    }

    public UUID getPlayerId() { return playerId; }
    public UpdateType getUpdateType() { return updateType; }
    public int getTitleId() { return titleId; }
    public String getEntityId() { return entityId; }
    public int getCount() { return count; }
}
