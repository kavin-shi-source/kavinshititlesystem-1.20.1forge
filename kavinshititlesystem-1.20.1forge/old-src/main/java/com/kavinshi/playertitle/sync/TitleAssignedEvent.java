package com.kavinshi.playertitle.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * 玩家获得新称号的跨服事件。
 * 当玩家完成条件或管理员授予称号时触发此事件。
 */
public class TitleAssignedEvent extends ClusterSyncEvent {
    private final int titleId;
    private final Instant assignedAt;
    
    /**
     * 创建称号分配事件。
     *
     * @param playerId 玩家UUID
     * @param revision 事件版本号
     * @param titleId 分配的称号ID
     * @param assignedAt 分配时间
     */
    public TitleAssignedEvent(UUID playerId, long revision, int titleId, Instant assignedAt) {
        super(playerId, revision, ClusterEventType.TITLE_ASSIGNED);
        this.titleId = titleId;
        this.assignedAt = assignedAt;
        validate();
    }
    
    /**
     * 创建称号分配事件（用于反序列化）。
     */
    public TitleAssignedEvent(UUID eventId, UUID playerId, long revision, Instant timestamp, 
                             int titleId, Instant assignedAt) {
        super(eventId, playerId, revision, timestamp, ClusterEventType.TITLE_ASSIGNED);
        this.titleId = titleId;
        this.assignedAt = assignedAt;
        validate();
    }
    
    /**
     * 获取分配的称号ID。
     *
     * @return 称号ID
     */
    public int getTitleId() {
        return titleId;
    }
    
    /**
     * 获取分配时间。
     *
     * @return 分配时间戳
     */
    public Instant getAssignedAt() {
        return assignedAt;
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
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt cannot be null");
        }
        if (assignedAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("assignedAt cannot be in the future");
        }
    }
    
    @Override
    public String toString() {
        return String.format("TitleAssignedEvent{eventId=%s, playerId=%s, revision=%d, titleId=%d, assignedAt=%s}",
            getEventId(), getPlayerId(), getRevision(), titleId, assignedAt);
    }
}