package com.kavinshi.playertitle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.title.TitleCondition;
import com.kavinshi.playertitle.title.TitleConditionType;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TitleEquipServiceTest {
    @Test
    void refusesToEquipLockedTitle() {
        PlayerTitleState state = new PlayerTitleState(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        TitleRegistry registry = registryWithTitle(3001, "Locked");
        TitleEquipService service = new TitleEquipService();

        EquipResult result = service.equip(state, registry, 3001);

        assertFalse(result.success());
        assertEquals("TITLE_NOT_UNLOCKED", result.reason());
        assertEquals(-1, state.getEquippedTitleId());
    }

    @Test
    void equipsUnlockedTitle() {
        PlayerTitleState state = new PlayerTitleState(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        state.unlockTitle(3002);
        state.markClean();
        TitleRegistry registry = registryWithTitle(3002, "Unlocked");
        TitleEquipService service = new TitleEquipService();

        EquipResult result = service.equip(state, registry, 3002);

        assertTrue(result.success());
        assertEquals("EQUIPPED", result.reason());
        assertEquals(3002, state.getEquippedTitleId());
        assertTrue(state.isDirty());
    }

    @Test
    void repeatedEquipIsIdempotent() {
        PlayerTitleState state = new PlayerTitleState(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        state.unlockTitle(3003);
        state.setEquippedTitleId(3003);
        state.markClean();
        TitleRegistry registry = registryWithTitle(3003, "Existing");
        TitleEquipService service = new TitleEquipService();

        EquipResult result = service.equip(state, registry, 3003);

        assertTrue(result.success());
        assertEquals("ALREADY_EQUIPPED", result.reason());
        assertEquals(3003, state.getEquippedTitleId());
        assertFalse(state.isDirty());
    }

    @Test
    void unequipClearsCurrentTitle() {
        PlayerTitleState state = new PlayerTitleState(UUID.fromString("88888888-8888-8888-8888-888888888888"));
        state.unlockTitle(3004);
        state.setEquippedTitleId(3004);
        state.markClean();
        TitleEquipService service = new TitleEquipService();

        EquipResult result = service.unequip(state);

        assertTrue(result.success());
        assertEquals("UNEQUIPPED", result.reason());
        assertEquals(-1, state.getEquippedTitleId());
        assertTrue(state.isDirty());
    }

    private static TitleRegistry registryWithTitle(int id, String name) {
        TitleRegistry registry = new TitleRegistry();
        registry.loadAll(List.of(
            new TitleDefinition(
                id,
                name,
                id,
                0xFFFFFF,
                List.of(new TitleCondition(TitleConditionType.SURVIVAL_TIME, "", 1)),
                "default"
            )
        ));
        return registry;
    }
}
