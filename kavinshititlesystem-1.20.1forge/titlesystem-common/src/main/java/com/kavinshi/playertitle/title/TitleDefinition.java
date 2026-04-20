package com.kavinshi.playertitle.title;

import java.util.List;
import java.util.Map;

public final class TitleDefinition {
    private final int id;
    private final String name;
    private final int displayOrder;
    private final int color;
    private final String chromaType;
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
        this.conditions = List.copyOf(conditions);
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

    public int getId() { return this.id; }
    public String getName() { return this.name; }
    public int getDisplayOrder() { return this.displayOrder; }
    public int getColor() { return this.color; }
    public String getChromaType() { return this.chromaType; }
    public ChromaType getChromaTypeEnum() { return ChromaType.fromString(this.chromaType); }
    public boolean hasChroma() { return getChromaTypeEnum().hasChroma(); }
    public List<TitleCondition> getConditions() { return this.conditions; }
    public List<TitleBuff> getBuffs() { return this.buffs; }
    public String getDescription() { return this.description; }
    public String getCategory() { return this.category; }
    public String getIcon() { return this.icon; }
    public String getIconColor() { return this.iconColor; }
    public TitleStyleMode getStyleMode() { return this.styleMode; }
    public List<String> getBaseColors() { return this.baseColors; }
    public TitleAnimationProfile getAnimationProfile() { return this.animationProfile; }
    public String getAuraEffect() { return this.auraEffect; }

    public boolean hasAura() {
        return this.auraEffect != null && !this.auraEffect.isEmpty();
    }

    public boolean areAllConditionsMet(Map<String, Integer> killCounts,
                                        Map<String, Integer> uuidKillCounts,
                                        int aliveMinutes, long bounty) {
        if (this.conditions.isEmpty()) {
            return false;
        }
        return this.conditions.stream()
            .allMatch(condition -> condition.isMet(killCounts, uuidKillCounts, aliveMinutes, bounty));
    }
}
