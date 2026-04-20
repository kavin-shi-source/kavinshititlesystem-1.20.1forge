package com.kavinshi.playertitle.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * 玩家称号进度更新的跨服事件。
 * 当玩家称号的进度（如击杀数、采集数等）发生变化时触发此事件。
 */
public class TitleProgressUpdatedEvent extends ClusterSyncEvent {
    private final int titleId;
    private final int oldProgress;
    private final int newProgress;
    private final int totalRequired;
    private final Instant updatedAt;
    
    /**
     * 创建称号进度更新事件。
     *
     * @param playerId 玩家UUID
     * @param revision 事件版本号
     * @param titleId 称号ID
     * @param oldProgress 旧进度值
     * @param newProgress 新进度值
     * @param totalRequired 所需总进度
     * @param updatedAt 更新时间
     */
    public TitleProgressUpdatedEvent(UUID playerId, long revision, int titleId,
                                   int oldProgress, int newProgress, int totalRequired,
                                   Instant updatedAt) {
        super(playerId, revision, ClusterEventType.TITLE_PROGRESS_UPDATED);
        this.titleId = titleId;
        this.oldProgress = oldProgress;
        this.newProgress = newProgress;
        this.totalRequired = totalRequired;
        this.updatedAt = updatedAt;
        validate();
    }
    
    /**
     * 创建称号进度更新事件（用于反序列化）。
     */
    public TitleProgressUpdatedEvent(UUID eventId, UUID playerId, long revision, Instant timestamp,
                                   int titleId, int oldProgress, int newProgress, int totalRequired,
                                   Instant updatedAt) {
        super(eventId, playerId, revision, timestamp, ClusterEventType.TITLE_PROGRESS_UPDATED);
        this.titleId = titleId;
        this.oldProgress = oldProgress;
        this.newProgress = newProgress;
        this.totalRequired = totalRequired;
        this.updatedAt = updatedAt;
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
     * 获取旧进度值。
     *
     * @return 旧进度
     */
    public int getOldProgress() {
        return oldProgress;
    }
    
    /**
     * 获取新进度值。
     *
     * @return 新进度
     */
    public int getNewProgress() {
        return newProgress;
    }
    
    /**
     * 获取所需总进度。
     *
     * @return 所需总进度
     */
    public int getTotalRequired() {
        return totalRequired;
    }
    
    /**
     * 获取更新时间。
     *
     * @return 更新时间戳
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * 获取进度百分比。
     *
     * @return 进度百分比（0.0到1.0）
     */
    public double getProgressPercentage() {
        if (totalRequired <= 0) return 1.0;
        return Math.min(1.0, Math.max(0.0, (double) newProgress / totalRequired));
    }
    
    /**
     * 检查进度是否完成。
     *
     * @return 如果新进度 >= 所需总进度返回true
     */
    public boolean isCompleted() {
        return totalRequired > 0 && newProgress >= totalRequired;
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
        if (oldProgress < 0) {
            throw new IllegalArgumentException("oldProgress cannot be negative");
        }
        if (newProgress < 0) {
            throw new IllegalArgumentException("newProgress cannot be negative");
        }
        if (totalRequired < 0) {
            throw new IllegalArgumentException("totalRequired cannot be negative");
        }
        if (newProgress < oldProgress) {
            throw new IllegalArgumentException("newProgress cannot be less than oldProgress");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt cannot be null");
        }
    }
    
    @Override
    public String toString() {
        return String.format("TitleProgressUpdatedEvent{eventId=%s, playerId=%s, revision=%d, " +
                           "titleId=%d, oldProgress=%d, newProgress=%d, totalRequired=%d, updatedAt=%s}",
            getEventId(), getPlayerId(), getRevision(), titleId,
            oldProgress, newProgress, totalRequired, updatedAt);
    }
}