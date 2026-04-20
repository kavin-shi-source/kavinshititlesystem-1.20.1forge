package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * 称号实时更新通知包。
 * 服务器发送给客户端，通知称号状态变化（解锁、装备、撤销等）。
 */
public class TitleUpdatePacket extends AbstractPacket {
    private static final Logger LOGGER = LogManager.getLogger(TitleUpdatePacket.class);
    
    /**
     * 更新类型枚举。
     */
    public enum UpdateType {
        TITLE_UNLOCKED(0),    // 称号解锁
        TITLE_EQUIPPED(1),    // 称号装备
        TITLE_REVOKED(2),     // 称号撤销
        KILL_COUNT_UPDATED(3), // 击杀计数更新
        ALIVE_TIME_UPDATED(4); // 存活时间更新
        
        private final int id;
        
        UpdateType(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
        
        public static UpdateType fromId(int id) {
            for (UpdateType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return TITLE_UNLOCKED; // 默认值
        }
    }
    
    private final UUID playerId;
    private final UpdateType updateType;
    private final int titleId;          // 相关称号ID（对于解锁、装备、撤销）
    private final String entityId;      // 相关实体ID（对于击杀计数更新）
    private final int count;            // 数量（击杀计数或存活时间）
    
    /**
     * 创建称号解锁/装备/撤销更新包。
     */
    public TitleUpdatePacket(UUID playerId, UpdateType updateType, int titleId) {
        this(playerId, updateType, titleId, "", 0);
    }
    
    /**
     * 创建击杀计数更新包。
     */
    public TitleUpdatePacket(UUID playerId, String entityId, int count) {
        this(playerId, UpdateType.KILL_COUNT_UPDATED, -1, entityId, count);
    }
    
    /**
     * 创建存活时间更新包。
     */
    public TitleUpdatePacket(UUID playerId, int aliveMinutes) {
        this(playerId, UpdateType.ALIVE_TIME_UPDATED, -1, "", aliveMinutes);
    }
    
    /**
     * 通用构造函数。
     */
    private TitleUpdatePacket(UUID playerId, UpdateType updateType, int titleId, String entityId, int count) {
        this.playerId = playerId;
        this.updateType = updateType;
        this.titleId = titleId;
        this.entityId = entityId != null ? entityId : "";
        this.count = count;
    }
    
    /**
     * 从字节缓冲区解码。
     */
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
        context.enqueueWork(() -> {
            LOGGER.debug("Received TitleUpdatePacket for player: {}, type: {}, titleId: {}, entityId: {}, count: {}",
                playerId, updateType, titleId, entityId, count);
            
            // TODO: 实际的处理逻辑应该在client模块中实现
            // 例如：ClientTitleUpdateService.handleUpdate(this);
        });
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public UpdateType getUpdateType() {
        return updateType;
    }
    
    public int getTitleId() {
        return titleId;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public int getCount() {
        return count;
    }
}