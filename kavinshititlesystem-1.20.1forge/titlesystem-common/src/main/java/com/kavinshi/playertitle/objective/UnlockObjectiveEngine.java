package com.kavinshi.playertitle.objective;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.title.TitleConditionIndex;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnlockObjectiveEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnlockObjectiveEngine.class);

    private final TitleRegistry registry;

    public UnlockObjectiveEngine(TitleRegistry registry) {
        this.registry = registry;
    }

    public List<Integer> evaluateKills(PlayerTitleState state, String entityId, boolean hostile) {
        TitleConditionIndex index = registry.getConditionIndex();
        if (index == null || entityId == null || entityId.isBlank() || !hostile) {
            return evaluateAll(state);
        }

        List<Integer> newlyUnlocked = new ArrayList<>();
        Set<Integer> checked = new HashSet<>();
        
        checkEntries(state, index.getTitlesForMobKill(entityId), newlyUnlocked, checked);
        checkEntries(state, index.getTitlesForAnyHostileKill(), newlyUnlocked, checked);
        
        return newlyUnlocked;
    }

    public List<Integer> evaluateSurvival(PlayerTitleState state) {
        TitleConditionIndex index = registry.getConditionIndex();
        if (index == null) {
            return evaluateAll(state);
        }

        List<Integer> newlyUnlocked = new ArrayList<>();
        Set<Integer> checked = new HashSet<>();
        
        checkEntries(state, index.getTitlesForSurvivalTime(), newlyUnlocked, checked);
        
        return newlyUnlocked;
    }

    public List<Integer> evaluateAll(PlayerTitleState state) {
        TitleConditionIndex index = registry.getConditionIndex();
        List<Integer> newlyUnlocked = new ArrayList<>();
        if (index != null) {
            checkEntries(state, index.getAllEntries(), newlyUnlocked, new HashSet<>());
            return newlyUnlocked;
        }

        Map<String, Integer> killCounts = state.getKillCounts();
        int aliveMinutes = state.getAliveMinutes();

        for (TitleDefinition definition : registry.getAllTitlesSorted()) {
            if (state.isTitleUnlocked(definition.getId())) {
                continue;
            }
            if (definition.areAllConditionsMet(killCounts, Map.of(), aliveMinutes, 0L)) {
                state.unlockTitle(definition.getId());
                newlyUnlocked.add(definition.getId());
            }
        }
        return newlyUnlocked;
    }

    private void checkEntries(PlayerTitleState state, List<TitleConditionIndex.IndexEntry> entries,
                              List<Integer> unlocked, Set<Integer> checked) {
        Map<String, Integer> killCounts = state.getKillCounts();
        int aliveMinutes = state.getAliveMinutes();

        for (TitleConditionIndex.IndexEntry entry : entries) {
            TitleDefinition definition = entry.getDefinition();
            int titleId = definition.getId();

            if (!checked.add(titleId)) continue;
            if (state.isTitleUnlocked(titleId)) continue;

            if (definition.areAllConditionsMet(killCounts, Map.of(), aliveMinutes, 0L)) {
                state.unlockTitle(titleId);
                unlocked.add(titleId);
            }
        }
    }
}