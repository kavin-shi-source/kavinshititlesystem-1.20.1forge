package com.kavinshi.playertitle.service;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.title.TitleRegistry;

public final class TitleEquipService {
    public EquipResult equip(PlayerTitleState state, TitleRegistry registry, int titleId) {
        if (registry.getTitle(titleId) == null) {
            return new EquipResult(false, "TITLE_NOT_FOUND", state.getEquippedTitleId());
        }

        if (!state.isTitleUnlocked(titleId)) {
            return new EquipResult(false, "TITLE_NOT_UNLOCKED", state.getEquippedTitleId());
        }

        if (state.getEquippedTitleId() == titleId) {
            return new EquipResult(true, "ALREADY_EQUIPPED", titleId);
        }

        state.setEquippedTitleId(titleId);
        return new EquipResult(true, "EQUIPPED", titleId);
    }

    public EquipResult unequip(PlayerTitleState state) {
        if (state.getEquippedTitleId() == -1) {
            return new EquipResult(true, "ALREADY_UNEQUIPPED", -1);
        }

        state.setEquippedTitleId(-1);
        return new EquipResult(true, "UNEQUIPPED", -1);
    }
}
