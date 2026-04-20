package com.kavinshi.playertitle.title;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TitleDefinitionTest {
    @Test
    void requiresAllConditionsToBeMet() {
        TitleDefinition definition = new TitleDefinition(
            1001,
            "Dungeon Breaker",
            1001,
            0xFFFFFF,
            List.of(
                new TitleCondition(TitleConditionType.KILL_MOB, "minecraft:warden", 2),
                new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 30)
            ),
            "boss"
        );

        assertFalse(definition.areAllConditionsMet(
            Map.of("minecraft:warden", 2),
            Map.of(),
            29,
            0L
        ));

        assertTrue(definition.areAllConditionsMet(
            Map.of("minecraft:warden", 2),
            Map.of(),
            30,
            0L
        ));
    }

    @Test
    void emptyConditionListDoesNotAutoUnlockTitle() {
        TitleDefinition definition = new TitleDefinition(
            1002,
            "Placeholder",
            1002,
            0xFFFFFF,
            List.of(),
            "system"
        );

        assertFalse(definition.areAllConditionsMet(
            Map.of(),
            Map.of(),
            999,
            999L
        ));
    }
}
