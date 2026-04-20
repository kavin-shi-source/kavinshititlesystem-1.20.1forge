package com.kavinshi.playertitle.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * 玩家称号属性更新的跨服事件。
 * 当玩家称号的图标、颜色、样式等属性发生变化时触发此事件。
 */
public class TitleUpdatedEvent extends ClusterSyncEvent {
    private final int titleId;
    private final String icon;
    private final String iconColor;
    private final String styleMode;
    private final String baseColors;
    private final String animationProfile;
    private final Instant updatedAt;
    
    /**
     * 创建称号更新事件。
     *
     * @param playerId 玩家UUID
     * @param revision 事件版本号
     * @param titleId 更新的称号ID
     * @param icon 图标标识符（可能为null表示未更改）
     * @param iconColor 图标颜色（可能为null表示未更改）
     * @param styleMode 样式模式（可能为null表示未更改）
     * @param baseColors 基础颜色（可能为null表示未更改）
     * @param animationProfile 动画配置（可能为null表示未更改）
     * @param updatedAt 更新时间
     */
    public TitleUpdatedEvent(UUID playerId, long revision, int titleId,
                           String icon, String iconColor, String styleMode,
                           String baseColors, String animationProfile, Instant updatedAt) {
        super(playerId, revision, ClusterEventType.TITLE_UPDATED);
        this.titleId = titleId;
        this.icon = icon;
        this.iconColor = iconColor;
        this.styleMode = styleMode;
        this.baseColors = baseColors;
        this.animationProfile = animationProfile;
        this.updatedAt = updatedAt;
        validate();
    }
    
    /**
     * 创建称号更新事件（用于反序列化）。
     */
    public TitleUpdatedEvent(UUID eventId, UUID playerId, long revision, Instant timestamp,
                           int titleId, String icon, String iconColor, String styleMode,
                           String baseColors, String animationProfile, Instant updatedAt) {
        super(eventId, playerId, revision, timestamp, ClusterEventType.TITLE_UPDATED);
        this.titleId = titleId;
        this.icon = icon;
        this.iconColor = iconColor;
        this.styleMode = styleMode;
        this.baseColors = baseColors;
        this.animationProfile = animationProfile;
        this.updatedAt = updatedAt;
        validate();
    }
    
    /**
     * 获取更新的称号ID。
     *
     * @return 称号ID
     */
    public int getTitleId() {
        return titleId;
    }
    
    /**
     * 获取图标标识符。
     *
     * @return 图标标识符，可能为null
     */
    public String getIcon() {
        return icon;
    }
    
    /**
     * 获取图标颜色。
     *
     * @return 图标颜色，可能为null
     */
    public String getIconColor() {
        return iconColor;
    }
    
    /**
     * 获取样式模式。
     *
     * @return 样式模式，可能为null
     */
    public String getStyleMode() {
        return styleMode;
    }
    
    /**
     * 获取基础颜色。
     *
     * @return 基础颜色，可能为null
     */
    public String getBaseColors() {
        return baseColors;
    }
    
    /**
     * 获取动画配置。
     *
     * @return 动画配置，可能为null
     */
    public String getAnimationProfile() {
        return animationProfile;
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
     * 检查是否有图标变更。
     *
     * @return 如果图标字段不为null返回true
     */
    public boolean hasIconChange() {
        return icon != null;
    }
    
    /**
     * 检查是否有图标颜色变更。
     *
     * @return 如果图标颜色字段不为null返回true
     */
    public boolean hasIconColorChange() {
        return iconColor != null;
    }
    
    /**
     * 检查是否有样式模式变更。
     *
     * @return 如果样式模式字段不为null返回true
     */
    public boolean hasStyleModeChange() {
        return styleMode != null;
    }
    
    /**
     * 检查是否有基础颜色变更。
     *
     * @return 如果基础颜色字段不为null返回true
     */
    public boolean hasBaseColorsChange() {
        return baseColors != null;
    }
    
    /**
     * 检查是否有动画配置变更。
     *
     * @return 如果动画配置字段不为null返回true
     */
    public boolean hasAnimationProfileChange() {
        return animationProfile != null;
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
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt cannot be null");
        }
        if (updatedAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("updatedAt cannot be in the future");
        }
        // 至少一个字段应该被更新
        if (icon == null && iconColor == null && styleMode == null && 
            baseColors == null && animationProfile == null) {
            throw new IllegalArgumentException("At least one field must be updated");
        }
    }
    
    @Override
    public String toString() {
        return String.format("TitleUpdatedEvent{eventId=%s, playerId=%s, revision=%d, titleId=%d, " +
                           "icon=%s, iconColor=%s, styleMode=%s, updatedAt=%s}",
            getEventId(), getPlayerId(), getRevision(), titleId,
            icon, iconColor, styleMode, updatedAt);
    }
}