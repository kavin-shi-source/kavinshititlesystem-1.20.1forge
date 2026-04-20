package com.kavinshi.playertitle.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerTitleStateTest {
    @Test
    void unlockEquipAndRevokeFlowKeepsStateConsistent() {
        PlayerTitleState state = new PlayerTitleState(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        assertTrue(state.isDirty());
        assertFalse(state.isTitleUnlocked(10));
        assertEquals(-1, state.getEquippedTitleId());

        state.markClean();
        state.unlockTitle(10);

        assertTrue(state.isTitleUnlocked(10));
        assertTrue(state.isDirty());

        state.markClean();
        state.setEquippedTitleId(10);
        assertEquals(10, state.getEquippedTitleId());
        assertTrue(state.isDirty());

        state.markClean();
        state.revokeTitle(10);
        assertFalse(state.isTitleUnlocked(10));
        assertEquals(-1, state.getEquippedTitleId());
        assertTrue(state.isDirty());
    }
}
