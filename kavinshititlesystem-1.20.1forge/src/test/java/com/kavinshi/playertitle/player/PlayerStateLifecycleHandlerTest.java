package com.kavinshi.playertitle.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerStateLifecycleHandlerTest {
    @Test
    void deathCloneResetsAliveMinutesButPreservesTitlesAndKills() {
        PlayerTitleState source = new PlayerTitleState(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        source.unlockTitle(21);
        source.setEquippedTitleId(21);
        source.addKill("minecraft:warden");
        source.setAliveMinutes(60);
        source.markClean();

        PlayerStateLifecycleHandler handler = new PlayerStateLifecycleHandler();

        PlayerTitleState cloned = handler.copyForClone(
            source,
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
            true
        );

        assertTrue(cloned.isTitleUnlocked(21));
        assertEquals(21, cloned.getEquippedTitleId());
        assertEquals(1, cloned.getKillCount("minecraft:warden"));
        assertEquals(0, cloned.getAliveMinutes());
    }

    @Test
    void nonDeathClonePreservesAliveMinutes() {
        PlayerTitleState source = new PlayerTitleState(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        source.unlockTitle(22);
        source.setAliveMinutes(35);
        source.markClean();

        PlayerStateLifecycleHandler handler = new PlayerStateLifecycleHandler();

        PlayerTitleState cloned = handler.copyForClone(
            source,
            UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
            false
        );

        assertTrue(cloned.isTitleUnlocked(22));
        assertEquals(35, cloned.getAliveMinutes());
    }
}
