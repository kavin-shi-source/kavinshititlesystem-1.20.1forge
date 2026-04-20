package com.kavinshi.playertitle.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * 玩家称号装备状态变更的跨服事件。
 * 当玩家装备或卸下称号时触发此事件。
 */
public class TitleEquipStateChangedEvent extends ClusterSyncEvent {
    private final int titleId;
    private final boolean equipped;
    private final Instant changedAt;
    
    /**
     * 创建称号装备状态变更事件。
     *
     * @param playerId 玩家UUID
     * @param revision 事件版本号
     * @param titleId 称号ID
     * @param equipped 是否装备（true表示装备，false表示卸下）
     * @param changedAt 变更时间
     */
    public TitleEquipStateChangedEvent(UUID playerId, long revision, int titleId,
                                     boolean equipped, Instant changedAt) {
        super(playerId, revision, ClusterEventType.TITLE_EQUIP_STATE_CHANGED);
        this.titleId = titleId;
        this.equipped = equipped;
        this.changedAt = changedAt;
        validate();
    }
    
    /**
     * 创建称号装备状态变更事件（用于反序列化）。
     */
    public TitleEquipStateChangedEvent(UUID eventId, UUID playerId, long revision, Instant timestamp,
                                     int titleId, boolean equipped, Instant changedAt) {
        super(eventId, playerId, revision, timestamp, ClusterEventType.TITLE_EQUIP_STATE_CHANGED);
        this.titleId = titleId;
        this.equipped = equipped;
        this.changedAt = changedAt;
        validate();
    }
    
    /**
     * 获取称号ID。
     *
     * @return 称号ID
     */
    public int getTitleId() {
        return titleId;
    }
    
    /**
     * 检查是否装备。
     *
     * @return true表示装备，false表示卸下
     */
    public boolean isEquipped() {
        return equipped;
    }
    
    /**
     * 获取变更时间。
     *
     * @return 变更时间戳
     */
    public Instant getChangedAt() {
        return changedAt;
    }
    
    /**
     * 验证事件数据。
     *
     * @throws IllegalArgumentException 如果数据无效
     */
    @Override
    public void validate() {
        super.validate();
        if (titleId <= 0) {
            throw new IllegalArgumentException("titleId must be positive");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt cannot be null");
        }
    }
    
    @Override
    public String toString() {
        return String.format("TitleEquipStateChangedEvent{eventId=%s, playerId=%s, revision=%d, " +
                           "titleId=%d, equipped=%s, changedAt=%s}",
            getEventId(), getPlayerId(), getRevision(), titleId, equipped, changedAt);
    }
}