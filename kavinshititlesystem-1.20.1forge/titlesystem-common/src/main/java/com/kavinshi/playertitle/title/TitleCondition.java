package com.kavinshi.playertitle.title;

import java.util.Map;

public final class TitleCondition {
    private final TitleConditionType type;
    private final String target;
    private final int requiredCount;

    public TitleCondition(TitleConditionType type, String target, int requiredCount) {
        if (type == null) throw new IllegalArgumentException("TitleCondition type cannot be null");
        this.type = type;
        this.target = target == null ? "" : target;
        this.requiredCount = requiredCount;
    }

    public TitleConditionType getType() {
        return this.type;
    }

    public String getTarget() {
        return this.target;
    }

    public int getRequiredCount() {
        return this.requiredCount;
    }

    public boolean isMet(
        Map<String, Integer> killCounts,
        Map<String, Integer> uuidKillCounts,
        int aliveMinutes,
        long bounty
    ) {
        return switch (this.type) {
            case KILL_ANY_HOSTILE -> killCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum() >= this.requiredCount;
            case KILL_MOB -> killCounts.getOrDefault(this.target.toLowerCase(), 0) >= this.requiredCount;
            case KILL_UUID -> uuidKillCounts.getOrDefault(this.target.toLowerCase(), 0) >= this.requiredCount;
            case SURVIVAL_TIME -> aliveMinutes >= this.requiredCount;
            case BOUNTY -> bounty >= this.requiredCount;
        };
    }
}
