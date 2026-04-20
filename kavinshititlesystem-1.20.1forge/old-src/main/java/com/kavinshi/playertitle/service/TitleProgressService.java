package com.kavinshi.playertitle.service;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.sync.ClusterEventBus;
import com.kavinshi.playertitle.sync.TitleEventFactory;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 称号进度服务，处理玩家进度记录和称号解锁。
 * 发布称号解锁事件到集群事件总线。
 */
public final class TitleProgressService {
    private final TitleEventFactory eventFactory;
    private final ClusterEventBus eventBus;
    
    /**
     * 创建称号进度服务。
     *
     * @param eventFactory 事件工厂
     * @param eventBus 事件总线
     */
    public TitleProgressService(TitleEventFactory eventFactory, ClusterEventBus eventBus) {
        this.eventFactory = eventFactory;
        this.eventBus = eventBus;
    }
    
    /**
     * 记录击杀并检查解锁。
     *
     * @param state 玩家称号状态
     * @param registry 称号注册表
     * @param entityId 实体ID
     * @param hostile 是否为敌对实体
     * @param entityUuid 实体UUID（可选）
     * @return 进度更新结果，包含新解锁的称号ID列表
     */
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

    /**
     * 记录存活时间并检查解锁。
     *
     * @param state 玩家称号状态
     * @param registry 称号注册表
     * @param aliveMinutes 存活分钟数
     * @return 进度更新结果，包含新解锁的称号ID列表
     */
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
                
                // 发布解锁事件
                try {
                    var event = eventFactory.createUnlockEvent(
                        state.getPlayerId(), definition.getId(), Instant.now()
                    );
                    eventBus.publish(event);
                } catch (Exception e) {
                    // 事件发布失败不应影响解锁操作，但应记录日志
                    System.err.println("Failed to publish unlock event for title " + definition.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return unlocked.isEmpty() ? ProgressUpdateResult.empty() : new ProgressUpdateResult(List.copyOf(unlocked));
    }
}