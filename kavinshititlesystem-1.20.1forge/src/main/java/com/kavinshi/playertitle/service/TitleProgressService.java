package com.kavinshi.playertitle.service;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import java.util.ArrayList;
import java.util.List;

public final class TitleProgressService {
    public ProgressUpdateResult recordKill(
        PlayerTitleState state,
        TitleRegistry registry,
        String entityId,
        boolean hostile,
        String entityUuid
    ) {
        if (hostile && entityId != null && !entityId.isBlank()) {
            state.addKill(entityId);
        }

        return evaluateUnlocks(state, registry);
    }

    public ProgressUpdateResult recordAliveMinutes(
        PlayerTitleState state,
        TitleRegistry registry,
        int aliveMinutes
    ) {
        state.setAliveMinutes(aliveMinutes);
        return evaluateUnlocks(state, registry);
    }

    private ProgressUpdateResult evaluateUnlocks(PlayerTitleState state, TitleRegistry registry) {
        List<Integer> unlocked = new ArrayList<>();

        for (TitleDefinition definition : registry.getAllTitlesSorted()) {
            if (state.isTitleUnlocked(definition.getId())) {
                continue;
            }

            if (definition.areAllConditionsMet(state.getKillCounts(), java.util.Map.of(), state.getAliveMinutes(), 0L)) {
                state.unlockTitle(definition.getId());
                unlocked.add(definition.getId());
            }
        }

        return unlocked.isEmpty() ? ProgressUpdateResult.empty() : new ProgressUpdateResult(List.copyOf(unlocked));
    }
}
