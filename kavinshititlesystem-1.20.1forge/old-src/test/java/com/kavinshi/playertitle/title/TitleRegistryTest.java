package com.kavinshi.playertitle.title;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TitleRegistryTest {
    @Test
    void sortsByDisplayOrderThenId() {
        TitleRegistry registry = new TitleRegistry();
        registry.loadAll(List.of(
            new TitleDefinition(3, "Gamma", 20, 0xFFFFFF, List.of(
                new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 1)
            ), "default"),
            new TitleDefinition(2, "Beta", 10, 0xFFFFFF, List.of(
                new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 1)
            ), "default"),
            new TitleDefinition(1, "Alpha", 10, 0xFFFFFF, List.of(
                new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 1)
            ), "default")
        ));

        assertEquals(List.of(1, 2, 3), registry.getAllTitlesSorted().stream().map(TitleDefinition::getId).toList());
    }
}
