package com.kavinshi.playertitle.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * 称号事件工厂，负责创建各种称号相关的跨服事件。
 * 确保事件数据的一致性和正确性。
 */
public class TitleEventFactory {
    private final ClusterRevisionService revisionService;
    
    public TitleEventFactory(ClusterRevisionService revisionService) {
        this.revisionService = revisionService;
    }
    
    /**
     * 创建称号解锁事件（玩家获得新称号）。
     *
     * @param playerId 玩家UUID
     * @param titleId 解锁的称号ID
     * @param unlockedAt 解锁时间
     * @return 称号解锁事件
     */
    public TitleAssignedEvent createUnlockEvent(UUID playerId, int titleId, Instant unlockedAt) {
        long revision = revisionService.incrementAndGet(playerId);
        return new TitleAssignedEvent(
            playerId, revision, titleId, unlockedAt
        );
    }
    
    /**
     * 创建称号装备事件。
     *
     * @param playerId 玩家UUID
     * @param titleId 装备的称号ID
     * @param equippedAt 装备时间
     * @return 称号装备状态变更事件
     */
    public TitleEquipStateChangedEvent createEquipEvent(UUID playerId, int titleId, Instant equippedAt) {
        long revision = revisionService.incrementAndGet(playerId);
        return new TitleEquipStateChangedEvent(
            playerId, revision, titleId, true, equippedAt
        );
    }
    
    /**
     * 创建称号卸下事件。
     *
     * @param playerId 玩家UUID
     * @param titleId 卸下的称号ID
     * @param unequippedAt 卸下时间
     * @return 称号装备状态变更事件
     */
    public TitleEquipStateChangedEvent createUnequipEvent(UUID playerId, int titleId, Instant unequippedAt) {
        long revision = revisionService.incrementAndGet(playerId);
        return new TitleEquipStateChangedEvent(
            playerId, revision, titleId, false, unequippedAt
        );
    }
    
    /**
     * 创建称号进度更新事件（如击杀数增加）。
     *
     * @param playerId 玩家UUID
     * @param titleId 称号ID
     * @param oldProgress 旧进度值
     * @param newProgress 新进度值
     * @param totalRequired 所需总进度
     * @param updatedAt 更新时间
     * @return 称号进度更新事件
     */
    public TitleProgressUpdatedEvent createProgressUpdateEvent(UUID playerId, int titleId,
                                                             int oldProgress, int newProgress, 
                                                             int totalRequired, Instant updatedAt) {
        long revision = revisionService.incrementAndGet(playerId);
        return new TitleProgressUpdatedEvent(
            playerId, revision, titleId, oldProgress, newProgress, totalRequired, updatedAt
        );
    }
    
    /**
     * 获取指定玩家的下一个版本号（不递增）。
     * 用于外部事件创建。
     *
     * @param playerId 玩家UUID
     * @return 下一个版本号
     */
    public long getNextRevision(UUID playerId) {
        return revisionService.getCurrentRevision(playerId) + 1;
    }
}