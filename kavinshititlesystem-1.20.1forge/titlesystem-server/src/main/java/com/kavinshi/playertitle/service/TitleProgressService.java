package com.kavinshi.playertitle.service;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.sync.ClusterEventBus;
import com.kavinshi.playertitle.sync.ClusterEventType;
import com.kavinshi.playertitle.sync.TitleAssignedEvent;
import com.kavinshi.playertitle.sync.TitleEventFactory;
import com.kavinshi.playertitle.sync.TitleRemovedEvent;
import com.kavinshi.playertitle.title.TitleConditionIndex;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
    private MinecraftServer server;

    public TitleProgressService(TitleEventFactory eventFactory, ClusterEventBus eventBus) {
        this.eventFactory = eventFactory;
        this.eventBus = eventBus;
    }

    public void onServerStarting(MinecraftServer server) {
        this.server = server;

        try {
            this.eventBus.subscribe(ClusterEventType.TITLE_ASSIGNED, event -> {
                if (!(event instanceof TitleAssignedEvent assignedEvent)) return;
                ServerPlayer player = this.server.getPlayerList().getPlayer(assignedEvent.getPlayerId());
                if (player == null) return;
                TitleCapability.get(player).ifPresent(state -> {
                    state.unlockTitle(assignedEvent.getTitleId());
                });
            });

            this.eventBus.subscribe(ClusterEventType.TITLE_REMOVED, event -> {
                if (!(event instanceof TitleRemovedEvent removedEvent)) return;
                ServerPlayer player = this.server.getPlayerList().getPlayer(removedEvent.getPlayerId());
                if (player == null) return;
                TitleCapability.get(player).ifPresent(state -> {
                    state.revokeTitle(removedEvent.getTitleId());
                });
            });

            LOGGER.info("TitleProgressService cross-server event consumers registered");
        } catch (ClusterEventBus.EventBusException e) {
            LOGGER.error("Failed to register TitleProgressService event consumers", e);
        }
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

        com.kavinshi.playertitle.objective.UnlockObjectiveEngine engine = new com.kavinshi.playertitle.objective.UnlockObjectiveEngine(registry);
        List<Integer> unlocked = engine.evaluateKills(state, entityId, hostile);
        
        for (int titleId : unlocked) {
            publishUnlockEvent(state, titleId);
        }

        return toResult(unlocked);
    }

    public ProgressUpdateResult recordAliveMinutes(
        PlayerTitleState state,
        TitleRegistry registry,
        int aliveMinutes
    ) {
        state.setAliveMinutes(aliveMinutes);

        com.kavinshi.playertitle.objective.UnlockObjectiveEngine engine = new com.kavinshi.playertitle.objective.UnlockObjectiveEngine(registry);
        List<Integer> unlocked = engine.evaluateSurvival(state);
        
        for (int titleId : unlocked) {
            publishUnlockEvent(state, titleId);
        }

        return toResult(unlocked);
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
