package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * 客户端请求同步数据的网络包。
 * 当客户端需要获取最新称号数据时发送此包到服务器。
 */
public class RequestSyncPacket extends AbstractPacket {
    private static final Logger LOGGER = LogManager.getLogger(RequestSyncPacket.class);
    
    private final UUID playerId;
    private final boolean fullSync; // 是否请求完整同步
    
    /**
     * 创建请求包。
     * 
     * @param playerId 请求者的玩家ID
     * @param fullSync 是否请求完整同步（包括击杀统计等详细数据）
     */
    public RequestSyncPacket(UUID playerId, boolean fullSync) {
        this.playerId = playerId;
        this.fullSync = fullSync;
    }
    
    /**
     * 从字节缓冲区解码。
     */
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
        context.enqueueWork(() -> {
            // 服务器处理：根据请求发送同步数据
            LOGGER.debug("Received sync request from player: {}, fullSync: {}",
                playerId, fullSync);
            
            // TODO: 实际的处理逻辑应该在server模块中实现
            // 例如：ServerTitleSyncService.handleSyncRequest(this);
        });
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public boolean isFullSync() {
        return fullSync;
    }
}