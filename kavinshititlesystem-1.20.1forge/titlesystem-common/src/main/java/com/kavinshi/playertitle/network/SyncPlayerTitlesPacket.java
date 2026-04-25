package com.kavinshi.playertitle.network;

import com.kavinshi.playertitle.title.CustomTitleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;

/**
 * 同步玩家标题数据包，用于在客户端和服务器之间同步玩家的完整标题状态。
 * 包含玩家解锁的标题ID、装备的标题ID、击杀计数、存活时间和自定义标题数据。
 */
@SuppressWarnings("null")
public class SyncPlayerTitlesPacket extends AbstractPacket {

    private final UUID playerId;
    private final Set<Integer> unlockedTitleIds;
    private final int equippedTitleId;
    private final Map<String, Integer> killCounts;
    private final int aliveMinutes;
    private final CustomTitleData customTitle;

    public SyncPlayerTitlesPacket(UUID playerId, Set<Integer> unlockedTitleIds, int equippedTitleId,
                                 Map<String, Integer> killCounts, int aliveMinutes, CustomTitleData customTitle) {
        this.playerId = playerId;
        this.unlockedTitleIds = new HashSet<>(unlockedTitleIds);
        this.equippedTitleId = equippedTitleId;
        this.killCounts = new HashMap<>(killCounts);
        this.aliveMinutes = aliveMinutes;
        this.customTitle = customTitle;
    }

    public SyncPlayerTitlesPacket(FriendlyByteBuf buffer) {
        this.playerId = buffer.readUUID();

        int unlockedCount = buffer.readVarInt();
        this.unlockedTitleIds = new HashSet<>(unlockedCount);
        for (int i = 0; i < unlockedCount; i++) {
            unlockedTitleIds.add(buffer.readVarInt());
        }

        this.equippedTitleId = buffer.readVarInt();

        int killCountsSize = buffer.readVarInt();
        this.killCounts = new HashMap<>(killCountsSize);
        for (int i = 0; i < killCountsSize; i++) {
            String entityId = readString(buffer);
            int count = buffer.readVarInt();
            killCounts.put(entityId, count);
        }

        this.aliveMinutes = buffer.readVarInt();

        String ctText = readString(buffer);
        int ctPerm = buffer.readVarInt();
        int ctColor1 = buffer.readInt();
        int ctColor2 = buffer.readInt();
        boolean ctUsing = buffer.readBoolean();
        long ctLastMod = buffer.readVarLong();
        this.customTitle = new CustomTitleData(ctText, ctPerm, ctColor1, ctColor2, ctUsing, ctLastMod);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);

        buffer.writeVarInt(unlockedTitleIds.size());
        for (int titleId : unlockedTitleIds) {
            buffer.writeVarInt(titleId);
        }

        buffer.writeVarInt(equippedTitleId);

        buffer.writeVarInt(killCounts.size());
        for (Map.Entry<String, Integer> entry : killCounts.entrySet()) {
            writeString(buffer, entry.getKey());
            buffer.writeVarInt(entry.getValue());
        }

        buffer.writeVarInt(aliveMinutes);

        writeString(buffer, customTitle.getText());
        buffer.writeVarInt(customTitle.getPermission());
        buffer.writeInt(customTitle.getColor1());
        buffer.writeInt(customTitle.getColor2());
        buffer.writeBoolean(customTitle.isUsingCustomTitle());
        buffer.writeVarLong(customTitle.getLastModifiedTime());
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        PacketHandlers.handleSyncPlayerTitles(playerId, unlockedTitleIds, equippedTitleId, killCounts, aliveMinutes, customTitle);
    }

    public UUID getPlayerId() { return playerId; }
    public Set<Integer> getUnlockedTitleIds() { return Collections.unmodifiableSet(unlockedTitleIds); }
    public int getEquippedTitleId() { return equippedTitleId; }
    public Map<String, Integer> getKillCounts() { return Collections.unmodifiableMap(killCounts); }
    public int getAliveMinutes() { return aliveMinutes; }
    public CustomTitleData getCustomTitle() { return customTitle; }

    public static SyncPlayerTitlesPacket fromPlayerTitleState(com.kavinshi.playertitle.player.PlayerTitleState state) {
        return new SyncPlayerTitlesPacket(
            state.getPlayerId(),
            state.getUnlockedTitleIds(),
            state.getEquippedTitleId(),
            state.getKillCounts(),
            state.getAliveMinutes(),
            state.getCustomTitle()
        );
    }
}
