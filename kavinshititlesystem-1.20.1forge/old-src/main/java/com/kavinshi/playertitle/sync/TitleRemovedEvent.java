package com.kavinshi.playertitle.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * 玩家称号被移除的跨服事件。
 * 当管理员移除玩家称号或称号过期时触发此事件。
 */
public class TitleRemovedEvent extends ClusterSyncEvent {
    private final int titleId;
    private final Instant removedAt;
    private final String reason;
    
    /**
     * 创建称号移除事件。
     *
     * @param playerId 玩家UUID
     * @param revision 事件版本号
     * @param titleId 被移除的称号ID
     * @param removedAt 移除时间
     * @param reason 移除原因（可选）
     */
    public TitleRemovedEvent(UUID playerId, long revision, int titleId, Instant removedAt, String reason) {
        super(playerId, revision, ClusterEventType.TITLE_REMOVED);
        this.titleId = titleId;
        this.removedAt = removedAt;
        this.reason = reason;
        validate();
    }
    
    /**
     * 创建称号移除事件（用于反序列化）。
     */
    public TitleRemovedEvent(UUID eventId, UUID playerId, long revision, Instant timestamp,
                           int titleId, Instant removedAt, String reason) {
        super(eventId, playerId, revision, timestamp, ClusterEventType.TITLE_REMOVED);
        this.titleId = titleId;
        this.removedAt = removedAt;
        this.reason = reason;
        validate();
    }
    
    /**
     * 获取被移除的称号ID。
     *
     * @return 称号ID
     */
    public int getTitleId() {
        return titleId;
    }
    
    /**
     * 获取移除时间。
     *
     * @return 移除时间戳
     */
    public Instant getRemovedAt() {
        return removedAt;
    }
    
    /**
     * 获取移除原因。
     *
     * @return 移除原因，可能为null
     */
    public String getReason() {
        return reason;
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
        if (removedAt == null) {
            throw new IllegalArgumentException("removedAt cannot be null");
        }
        if (removedAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("removedAt cannot be in the future");
        }
    }
    
    @Override
    public String toString() {
        return String.format("TitleRemovedEvent{eventId=%s, playerId=%s, revision=%d, titleId=%d, removedAt=%s, reason=%s}",
            getEventId(), getPlayerId(), getRevision(), titleId, removedAt, reason);
    }
}