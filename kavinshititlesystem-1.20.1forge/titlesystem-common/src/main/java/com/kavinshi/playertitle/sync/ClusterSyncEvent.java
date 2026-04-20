package com.kavinshi.playertitle.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * 跨服同步事件的抽象基类，包含所有事件的公共字段。
 * 事件使用版本号（revision）确保顺序和幂等性。
 * 每个事件必须包含玩家UUID和事件类型，以及唯一标识符。
 */
public abstract class ClusterSyncEvent {
    private final UUID eventId;
    private final UUID playerId;
    private final long revision;
    private final Instant timestamp;
    private final ClusterEventType eventType;
    
    /**
     * 创建跨服同步事件。
     *
     * @param playerId 玩家UUID
     * @param revision 事件版本号（通常为递增的序列号）
     * @param eventType 事件类型
     */
    protected ClusterSyncEvent(UUID playerId, long revision, ClusterEventType eventType) {
        this.eventId = UUID.randomUUID();
        this.playerId = playerId;
        this.revision = revision;
        this.timestamp = Instant.now();
        this.eventType = eventType;
    }
    
    /**
     * 创建跨服同步事件（指定事件ID和时间戳，用于反序列化）。
     *
     * @param eventId 事件唯一标识符
     * @param playerId 玩家UUID
     * @param revision 事件版本号
     * @param timestamp 事件时间戳
     * @param eventType 事件类型
     */
    protected ClusterSyncEvent(UUID eventId, UUID playerId, long revision, Instant timestamp, ClusterEventType eventType) {
        this.eventId = eventId;
        this.playerId = playerId;
        this.revision = revision;
        this.timestamp = timestamp;
        this.eventType = eventType;
    }
    
    /**
     * 获取事件唯一标识符。
     *
     * @return 事件UUID
     */
    public UUID getEventId() {
        return eventId;
    }
    
    /**
     * 获取玩家UUID。
     *
     * @return 玩家UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * 获取事件版本号。
     * 版本号用于确保事件顺序和幂等性，每个玩家的版本号应单调递增。
     *
     * @return 事件版本号
     */
    public long getRevision() {
        return revision;
    }
    
    /**
     * 获取事件时间戳。
     *
     * @return 事件创建时间
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取事件类型。
     *
     * @return 事件类型枚举
     */
    public ClusterEventType getEventType() {
        return eventType;
    }
    
    /**
     * 获取事件的字符串表示，用于日志和调试。
     *
     * @return 事件字符串表示
     */
    @Override
    public String toString() {
        return String.format("%s{eventId=%s, playerId=%s, revision=%d, timestamp=%s}",
            getClass().getSimpleName(), eventId, playerId, revision, timestamp);
    }
    
    /**
     * 验证事件数据的有效性。
     * 子类可以覆盖此方法以添加额外的验证逻辑。
     *
     * @throws IllegalArgumentException 如果事件数据无效
     */
    public void validate() {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType cannot be null");
        }
    }
}