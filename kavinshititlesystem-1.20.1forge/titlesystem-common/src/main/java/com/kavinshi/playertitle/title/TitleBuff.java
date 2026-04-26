package com.kavinshi.playertitle.title;

/**
 * 标题增益效果类，表示称号提供的属性增益。
 * 包含增益类型、数值和目标实体等信息。
 */
public final class TitleBuff {
    private final BuffType type;
    private final double value;
    private final String target;

    public TitleBuff(BuffType type, double value, String target) {
        if (type == null) throw new IllegalArgumentException("TitleBuff type cannot be null");
        this.type = type;
        this.value = value;
        this.target = target == null ? "" : target;
    }

    public BuffType getType() { return this.type; }
    public double getValue() { return this.value; }
    public String getTarget() { return this.target; }

    public boolean appliesToTarget(String entityType) {
        if (this.type != BuffType.DAMAGE_MULTIPLIER) return true;
        if (this.target.isEmpty() || this.target.equalsIgnoreCase("all")) return true;
        return this.target.equalsIgnoreCase(entityType);
    }

    public static BuffType typeFromString(String name) {
        try {
            return BuffType.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return BuffType.ATTACK_DAMAGE;
        }
    }

    /**
     * 增益类型枚举，定义称号可提供的各种属性增益。
     */
    public enum BuffType {
        ATTACK_DAMAGE, DAMAGE_MULTIPLIER, MAX_HEALTH, SPEED, ARMOR, ARMOR_TOUGHNESS, LUCK
    }
}
