package com.kavinshi.playertitle.service;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.sync.ClusterEventBus;
import com.kavinshi.playertitle.sync.TitleEventFactory;
import com.kavinshi.playertitle.title.TitleConditionIndex;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TitleProgressService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TitleProgressService.class);

    private final TitleEventFactory eventFactory;
    private final ClusterEventBus eventBus;

    public TitleProgressService(TitleEventFactory eventFactory, ClusterEventBus eventBus) {
        this.eventFactory = eventFactory;
        this.eventBus = eventBus;
    }

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

        TitleConditionIndex index = registry.getConditionIndex();
        if (index != null && entityId != null && !entityId.isBlank() && hostile) {
            return evaluateUnlocksForKill(state, index, entityId);
        }

        return evaluateAllUnlocks(state, registry);
    }

    public ProgressUpdateResult recordAliveMinutes(
        PlayerTitleState state,
        TitleRegistry registry,
        int aliveMinutes
    ) {
        state.setAliveMinutes(aliveMinutes);

        TitleConditionIndex index = registry.getConditionIndex();
        if (index != null) {
            return evaluateUnlocksForSurvivalTime(state, index);
        }

        return evaluateAllUnlocks(state, registry);
    }

    private ProgressUpdateResult evaluateAllUnlocks(PlayerTitleState state, TitleRegistry registry) {
        TitleConditionIndex index = registry.getConditionIndex();
        if (index != null) {
            List<Integer> unlocked = new ArrayList<>();
            checkEntries(state, index.getAllEntries(), unlocked, new HashSet<>());
            return toResult(unlocked);
        }

        List<Integer> unlocked = new ArrayList<>();
        Map<String, Integer> killCounts = state.getKillCounts();
        int aliveMinutes = state.getAliveMinutes();

        for (TitleDefinition definition : registry.getAllTitlesSorted()) {
            if (state.isTitleUnlocked(definition.getId())) {
                continue;
            }
            if (definition.areAllConditionsMet(killCounts, Map.of(), aliveMinutes, 0L)) {
                state.unlockTitle(definition.getId());
                unlocked.add(definition.getId());
                publishUnlockEvent(state, definition.getId());
            }
        }

        return toResult(unlocked);
    }

    private ProgressUpdateResult evaluateUnlocksForKill(PlayerTitleState state, TitleConditionIndex index, String entityId) {
        List<Integer> unlocked = new ArrayList<>();
        Set<Integer> checked = new HashSet<>();
        checkEntries(state, index.getTitlesForMobKill(entityId), unlocked, checked);
        checkEntries(state, index.getTitlesForAnyHostileKill(), unlocked, checked);
        return toResult(unlocked);
    }

    private ProgressUpdateResult evaluateUnlocksForSurvivalTime(PlayerTitleState state, TitleConditionIndex index) {
        List<Integer> unlocked = new ArrayList<>();
        Set<Integer> checked = new HashSet<>();
        checkEntries(state, index.getTitlesForSurvivalTime(), unlocked, checked);
        return toResult(unlocked);
    }

    private void checkEntries(PlayerTitleState state, List<TitleConditionIndex.IndexEntry> entries,
                              List<Integer> unlocked, Set<Integer> checked) {
        Map<String, Integer> killCounts = state.getKillCounts();
        int aliveMinutes = state.getAliveMinutes();

        for (TitleConditionIndex.IndexEntry entry : entries) {
            TitleDefinition definition = entry.getDefinition();
            int titleId = definition.getId();

            if (!checked.add(titleId)) {
                continue;
            }

            if (state.isTitleUnlocked(titleId)) {
                continue;
            }
            if (definition.areAllConditionsMet(killCounts, Map.of(), aliveMinutes, 0L)) {
                state.unlockTitle(titleId);
                unlocked.add(titleId);
                publishUnlockEvent(state, titleId);
            }
        }
    }

    private ProgressUpdateResult toResult(List<Integer> unlocked) {
        return unlocked.isEmpty() ? ProgressUpdateResult.empty() : new ProgressUpdateResult(List.copyOf(unlocked));
    }

    private void publishUnlockEvent(PlayerTitleState state, int titleId) {
        try {
            var event = eventFactory.createUnlockEvent(state.getPlayerId(), titleId, Instant.now());
            eventBus.publish(event);
        } catch (Exception e) {
            LOGGER.error("Failed to publish unlock event for title {}: {}", titleId, e.getMessage());
        }
    }
}
