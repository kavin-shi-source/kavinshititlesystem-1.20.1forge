package com.kavinshi.playertitle.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class ForgePlayerTitleStateStoreTest {
    @Test
    void roundTripsStateThroughCompoundTag() {
        PlayerTitleState original = new PlayerTitleState(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        original.unlockTitle(11);
        original.unlockTitle(12);
        original.addKill("minecraft:warden");
        original.addKill("minecraft:warden");
        original.addKill("minecraft:zombie");
        original.setAliveMinutes(45);
        original.setEquippedTitleId(12);
        original.markClean();

        ForgePlayerTitleStateStore store = new ForgePlayerTitleStateStore();

        CompoundTag tag = store.write(original);
        PlayerTitleState restored = store.read(original.getPlayerId(), tag);

        assertTrue(restored.isTitleUnlocked(11));
        assertTrue(restored.isTitleUnlocked(12));
        assertEquals(12, restored.getEquippedTitleId());
        assertEquals(2, restored.getKillCount("minecraft:warden"));
        assertEquals(1, restored.getKillCount("minecraft:zombie"));
        assertEquals(45, restored.getAliveMinutes());
        assertFalse(restored.isDirty());
    }
}
