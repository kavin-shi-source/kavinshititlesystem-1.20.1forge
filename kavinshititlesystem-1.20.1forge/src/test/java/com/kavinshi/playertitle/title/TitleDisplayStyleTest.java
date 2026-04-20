package com.kavinshi.playertitle.title;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TitleDisplayStyleTest {
    @Test
    void storesPlainDisplayMetadata() {
        TitleDefinition definition = new TitleDefinition(
            6001,
            "Guardian",
            10,
            0xFFFFFF,
            List.of(new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 1)),
            "default",
            "✦",
            "#ffffff",
            TitleStyleMode.PLAIN,
            List.of(),
            null
        );

        assertEquals("✦", definition.getIcon());
        assertEquals("#ffffff", definition.getIconColor());
        assertEquals(TitleStyleMode.PLAIN, definition.getStyleMode());
    }

    @Test
    void storesStaticGradientMetadata() {
        TitleDefinition definition = new TitleDefinition(
            6002,
            "Ancient Warden",
            20,
            0xFFFFFF,
            List.of(new TitleCondition(TitleConditionType.KILL_MOB, "minecraft:warden", 2)),
            "combat",
            "⚔",
            "#ffaa00",
            TitleStyleMode.STATIC_GRADIENT,
            List.of("#ff0000", "#0000ff"),
            null
        );

        assertEquals(List.of("#ff0000", "#0000ff"), definition.getBaseColors());
        assertEquals(TitleStyleMode.STATIC_GRADIENT, definition.getStyleMode());
    }

    @Test
    void storesAnimatedChromaMetadata() {
        TitleAnimationProfile profile = new TitleAnimationProfile(1500, 2);
        TitleDefinition definition = new TitleDefinition(
            6003,
            "Rainbow Legend",
            30,
            0xFFFFFF,
            List.of(new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 60)),
            "special",
            "☄",
            "#ff00ff",
            TitleStyleMode.ANIMATED_CHROMA,
            List.of("#ff0000", "#00ff00", "#0000ff"),
            profile
        );

        assertEquals(TitleStyleMode.ANIMATED_CHROMA, definition.getStyleMode());
        assertEquals(1500, definition.getAnimationProfile().cycleMillis());
        assertEquals(2, definition.getAnimationProfile().stepSize());
    }
}
