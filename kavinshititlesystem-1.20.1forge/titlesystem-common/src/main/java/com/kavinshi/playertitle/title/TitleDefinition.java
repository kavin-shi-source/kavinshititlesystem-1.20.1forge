package com.kavinshi.playertitle.title;

import java.util.List;
import java.util.Map;

/**
 * 标题定义类，表示一个称号的完整定义信息。
 * 包含称号的基本属性、解锁条件、增益效果、样式和动画配置等。
 */
public final class TitleDefinition {
    private final int id;
    private final String name;
    private final int displayOrder;
    private final int color;
    private final String chromaType;
    private final ChromaType chromaTypeEnum;
    private final List<TitleCondition> conditions;
    private final List<TitleBuff> buffs;
    private final String description;
    private final String category;
    private final String icon;
    private final String iconColor;
    private final TitleStyleMode styleMode;
    private final List<String> baseColors;
    private final TitleAnimationProfile animationProfile;
    private final String auraEffect;

    public TitleDefinition(int id, String name, int displayOrder, int color,
                           List<TitleCondition> conditions, String category) {
        this(id, name, displayOrder, color, "NONE", conditions, List.of(), "", category,
             "", "#ffffff", TitleStyleMode.PLAIN, List.of(), null, null);
    }

    public TitleDefinition(int id, String name, int displayOrder, int color,
                           String chromaType, List<TitleCondition> conditions,
                           List<TitleBuff> buffs, String description, String category,
                           String icon, String iconColor, TitleStyleMode styleMode,
                           List<String> baseColors, TitleAnimationProfile animationProfile,
                           String auraEffect) {
        this.id = id;
        this.name = name;
        this.displayOrder = displayOrder;
        this.color = color;
        this.chromaType = chromaType == null ? "NONE" : chromaType;
        this.chromaTypeEnum = ChromaType.fromString(this.chromaType);
        this.conditions = conditions == null ? List.of() : List.copyOf(conditions);
        this.buffs = buffs == null ? List.of() : List.copyOf(buffs);
        this.description = description == null ? "" : description;
        this.category = category == null ? "default" : category;
        this.icon = icon == null ? "" : icon;
        this.iconColor = iconColor == null ? "#ffffff" : iconColor;
        this.styleMode = styleMode == null ? TitleStyleMode.PLAIN : styleMode;
        this.baseColors = baseColors == null ? List.of() : List.copyOf(baseColors);
        this.animationProfile = animationProfile;
        this.auraEffect = auraEffect;
    }

    /** 返回标题ID。 */
    public int getId() { return this.id; }
    /** 返回标题名称。 */
    public String getName() { return this.name; }
    /** 返回显示顺序，数值越小越靠前。 */
    public int getDisplayOrder() { return this.displayOrder; }
    /** 返回标题颜色值（RGB整数）。 */
    public int getColor() { return this.color; }
    /** 返回色彩类型字符串。 */
    public String getChromaType() { return this.chromaType; }
    /** 返回色彩类型枚举。 */
    public ChromaType getChromaTypeEnum() { return this.chromaTypeEnum; }
    /** 检查是否具有染色效果。 */
    public boolean hasChroma() { return getChromaTypeEnum().hasChroma(); }
    /** 返回解锁条件列表。 */
    public List<TitleCondition> getConditions() { return this.conditions; }
    /** 返回增益效果列表。 */
    public List<TitleBuff> getBuffs() { return this.buffs; }
    /** 返回标题描述。 */
    public String getDescription() { return this.description; }
    /** 返回标题分类。 */
    public String getCategory() { return this.category; }
    /** 返回图标资源路径。 */
    public String getIcon() { return this.icon; }
    /** 返回图标颜色。 */
    public String getIconColor() { return this.iconColor; }
    /** 返回标题样式模式。 */
    public TitleStyleMode getStyleMode() { return this.styleMode; }
    /** 返回基础颜色列表。 */
    public List<String> getBaseColors() { return this.baseColors; }
    /** 返回动画配置。 */
    public TitleAnimationProfile getAnimationProfile() { return this.animationProfile; }
    /** 返回光环效果。 */
    public String getAuraEffect() { return this.auraEffect; }

    /**
     * 检查标题是否具有光环效果。
     * 
     * @return 如果光环效果不为空且非空字符串，则返回true
     */
    public boolean hasAura() {
        return this.auraEffect != null && !this.auraEffect.isEmpty();
    }

    /**
     * 检查是否满足所有解锁条件。
     * 
     * @param killCounts 普通生物击杀计数映射
     * @param uuidKillCounts UUID生物击杀计数映射
     * @param aliveMinutes 存活时间（分钟）
     * @param bounty 赏金数量
     * @return 如果所有条件都满足则返回true，如果没有任何条件则返回true（无条件限制视为已满足）
     */
    public boolean areAllConditionsMet(Map<String, Integer> killCounts,
                                        Map<String, Integer> uuidKillCounts,
                                        int aliveMinutes, long bounty) {
        if (this.conditions.isEmpty()) {
            return true;
        }
        return this.conditions.stream()
            .allMatch(condition -> condition.isMet(killCounts, uuidKillCounts, aliveMinutes, bounty));
    }
}
