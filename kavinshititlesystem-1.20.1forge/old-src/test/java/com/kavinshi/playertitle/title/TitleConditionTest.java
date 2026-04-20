package com.kavinshi.playertitle.title;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TitleConditionTest {
    @Test
    void meetsAnyHostileKillConditionWhenTotalKillsReachRequiredCount() {
        TitleCondition condition = new TitleCondition(TitleConditionType.KILL_ANY_HOSTILE, "", 5);

        boolean matched = condition.isMet(
            Map.of("minecraft:zombie", 2, "minecraft:skeleton", 3),
            Map.of(),
            0,
            0L
        );

        assertTrue(matched);
    }

    @Test
    void doesNotMeetMobKillConditionWhenSpecificMobCountIsInsufficient() {
        TitleCondition condition = new TitleCondition(TitleConditionType.KILL_MOB, "minecraft:warden", 2);

        boolean matched = condition.isMet(
            Map.of("minecraft:warden", 1, "minecraft:zombie", 20),
            Map.of(),
            0,
            0L
        );

        assertFalse(matched);
    }

    @Test
    void meetsSurvivalTimeConditionWhenAliveMinutesReachThreshold() {
        TitleCondition condition = new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 30);

        boolean matched = condition.isMet(
            Map.of(),
            Map.of(),
            30,
            0L
        );

        assertTrue(matched);
    }
}
