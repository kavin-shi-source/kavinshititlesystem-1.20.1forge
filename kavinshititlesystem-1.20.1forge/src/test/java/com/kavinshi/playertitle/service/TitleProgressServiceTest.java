package com.kavinshi.playertitle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.title.TitleCondition;
import com.kavinshi.playertitle.title.TitleConditionType;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TitleProgressServiceTest {
    @Test
    void unlocksMobKillTitleWhenRequiredKillsAreReached() {
        TitleRegistry registry = new TitleRegistry();
        registry.loadAll(List.of(
            new TitleDefinition(
                2001,
                "Warden Hunter",
                2001,
                0xFFFFFF,
                List.of(new TitleCondition(TitleConditionType.KILL_MOB, "minecraft:warden", 2)),
                "combat"
            )
        ));

        PlayerTitleState state = new PlayerTitleState(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        TitleProgressService service = new TitleProgressService();

        ProgressUpdateResult first = service.recordKill(state, registry, "minecraft:warden", true, null);
        ProgressUpdateResult second = service.recordKill(state, registry, "minecraft:warden", true, null);

        assertTrue(first.unlockedTitleIds().isEmpty());
        assertEquals(List.of(2001), second.unlockedTitleIds());
        assertTrue(state.isTitleUnlocked(2001));
        assertEquals(2, state.getKillCount("minecraft:warden"));
    }

    @Test
    void doesNotUnlockSameTitleTwiceAfterAlreadyUnlocked() {
        TitleRegistry registry = new TitleRegistry();
        registry.loadAll(List.of(
            new TitleDefinition(
                2002,
                "Zombie Cleaner",
                2002,
                0xFFFFFF,
                List.of(new TitleCondition(TitleConditionType.KILL_MOB, "minecraft:zombie", 1)),
                "combat"
            )
        ));

        PlayerTitleState state = new PlayerTitleState(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        TitleProgressService service = new TitleProgressService();

        ProgressUpdateResult first = service.recordKill(state, registry, "minecraft:zombie", true, null);
        ProgressUpdateResult second = service.recordKill(state, registry, "minecraft:zombie", true, null);

        assertEquals(List.of(2002), first.unlockedTitleIds());
        assertTrue(second.unlockedTitleIds().isEmpty());
        assertEquals(2, state.getKillCount("minecraft:zombie"));
    }

    @Test
    void unlocksSurvivalTitleWhenAliveMinutesReachThreshold() {
        TitleRegistry registry = new TitleRegistry();
        registry.loadAll(List.of(
            new TitleDefinition(
                2003,
                "Still Standing",
                2003,
                0xFFFFFF,
                List.of(new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 30)),
                "survival"
            )
        ));

        PlayerTitleState state = new PlayerTitleState(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        TitleProgressService service = new TitleProgressService();

        ProgressUpdateResult result = service.recordAliveMinutes(state, registry, 30);

        assertEquals(List.of(2003), result.unlockedTitleIds());
        assertEquals(30, state.getAliveMinutes());
        assertTrue(state.isTitleUnlocked(2003));
    }
}
