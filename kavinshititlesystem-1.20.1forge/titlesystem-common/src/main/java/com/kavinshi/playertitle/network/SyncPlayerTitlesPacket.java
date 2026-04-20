package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Supplier;

/**
 * 同步玩家称号状态的网络包。
 * 服务器发送给客户端，用于更新客户端的称号显示数据。
 */
public class SyncPlayerTitlesPacket extends AbstractPacket {
    private static final Logger LOGGER = LogManager.getLogger(SyncPlayerTitlesPacket.class);
    
    private final UUID playerId;
    private final Set<Integer> unlockedTitleIds;
    private final int equippedTitleId;
    private final Map<String, Integer> killCounts;
    private final int aliveMinutes;
    
    /**
     * 创建同步包（用于发送方）。
     */
    public SyncPlayerTitlesPacket(UUID playerId, Set<Integer> unlockedTitleIds, int equippedTitleId,
                                 Map<String, Integer> killCounts, int aliveMinutes) {
        this.playerId = playerId;
        this.unlockedTitleIds = new HashSet<>(unlockedTitleIds);
        this.equippedTitleId = equippedTitleId;
        this.killCounts = new HashMap<>(killCounts);
        this.aliveMinutes = aliveMinutes;
    }
    
    /**
     * 从字节缓冲区解码（用于接收方）。
     */
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
    }
    
    @Override
    public void handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            // 客户端处理：更新本地缓存
            // 注意：这个处理逻辑应该在client模块中实现
            // 这里只是记录接收到的数据
            LOGGER.debug("Received SyncPlayerTitlesPacket for player: {}, unlocked: {}, equipped: {}",
                playerId, unlockedTitleIds.size(), equippedTitleId);
            
            // TODO: 实际的处理逻辑应该在client模块中实现
            // 例如：ClientTitleSyncService.handleSyncPacket(this);
        });
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public Set<Integer> getUnlockedTitleIds() {
        return Collections.unmodifiableSet(unlockedTitleIds);
    }
    
    public int getEquippedTitleId() {
        return equippedTitleId;
    }
    
    public Map<String, Integer> getKillCounts() {
        return Collections.unmodifiableMap(killCounts);
    }
    
    public int getAliveMinutes() {
        return aliveMinutes;
    }
    
    /**
     * 创建基于PlayerTitleState的同步包。
     */
    public static SyncPlayerTitlesPacket fromPlayerTitleState(com.kavinshi.playertitle.player.PlayerTitleState state) {
        return new SyncPlayerTitlesPacket(
            state.getPlayerId(),
            state.getUnlockedTitleIds(),
            state.getEquippedTitleId(),
            state.getKillCounts(),
            state.getAliveMinutes()
        );
    }
}