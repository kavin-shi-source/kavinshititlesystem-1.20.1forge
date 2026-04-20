package com.kavinshi.playertitle.title;

import java.util.List;
import java.util.Map;

public final class TitleDefinition {
    private final int id;
    private final String name;
    private final int displayOrder;
    private final int color;
    private final List<TitleCondition> conditions;
    private final String category;
    private final String icon;
    private final String iconColor;
    private final TitleStyleMode styleMode;
    private final List<String> baseColors;
    private final TitleAnimationProfile animationProfile;

    public TitleDefinition(
        int id,
        String name,
        int displayOrder,
        int color,
        List<TitleCondition> conditions,
        String category
    ) {
        this(id, name, displayOrder, color, conditions, category, "", "#ffffff", TitleStyleMode.PLAIN, List.of(), null);
    }

    public TitleDefinition(
        int id,
        String name,
        int displayOrder,
        int color,
        List<TitleCondition> conditions,
        String category,
        String icon,
        String iconColor,
        TitleStyleMode styleMode,
        List<String> baseColors,
        TitleAnimationProfile animationProfile
    ) {
        this.id = id;
        this.name = name;
        this.displayOrder = displayOrder;
        this.color = color;
        this.conditions = List.copyOf(conditions);
        this.category = category == null ? "default" : category;
        this.icon = icon == null ? "" : icon;
        this.iconColor = iconColor == null ? "#ffffff" : iconColor;
        this.styleMode = styleMode == null ? TitleStyleMode.PLAIN : styleMode;
        this.baseColors = baseColors == null ? List.of() : List.copyOf(baseColors);
        this.animationProfile = animationProfile;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public int getColor() {
        return this.color;
    }

    public List<TitleCondition> getConditions() {
        return this.conditions;
    }

    public String getCategory() {
        return this.category;
    }

    public String getIcon() {
        return this.icon;
    }

    public String getIconColor() {
        return this.iconColor;
    }

    public TitleStyleMode getStyleMode() {
        return this.styleMode;
    }

    public List<String> getBaseColors() {
        return this.baseColors;
    }

    public TitleAnimationProfile getAnimationProfile() {
        return this.animationProfile;
    }

    public boolean areAllConditionsMet(
        Map<String, Integer> killCounts,
        Map<String, Integer> uuidKillCounts,
        int aliveMinutes,
        long bounty
    ) {
        if (this.conditions.isEmpty()) {
            return false;
        }

        return this.conditions.stream()
            .allMatch(condition -> condition.isMet(killCounts, uuidKillCounts, aliveMinutes, bounty));
    }
}
