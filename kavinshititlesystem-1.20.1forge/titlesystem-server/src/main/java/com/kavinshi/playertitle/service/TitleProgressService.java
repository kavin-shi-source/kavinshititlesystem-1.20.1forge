package com.kavinshi.playertitle.service;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.sync.ClusterEventBus;
import com.kavinshi.playertitle.sync.TitleEventFactory;
import com.kavinshi.playertitle.title.TitleConditionIndex;
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

        // 尝试使用索引优化版本
        TitleConditionIndex index = registry.getConditionIndex();
        if (index != null && entityId != null && !entityId.isBlank() && hostile) {
            return evaluateUnlocksForKill(state, registry, index, entityId);
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
        
        // 尝试使用索引优化版本
        TitleConditionIndex index = registry.getConditionIndex();
        if (index != null) {
            return evaluateUnlocksForSurvivalTime(state, registry, index);
        }
        
        return evaluateUnlocks(state, registry);
    }

    private ProgressUpdateResult evaluateUnlocks(PlayerTitleState state, TitleRegistry registry) {
        // 尝试使用索引优化版本，如果索引可用
        TitleConditionIndex index = registry.getConditionIndex();
        if (index != null) {
            return evaluateUnlocksWithIndex(state, registry, index);
        }
        
        // 回退到原始遍历方法
        return evaluateUnlocksLegacy(state, registry);
    }
    
    private ProgressUpdateResult evaluateUnlocksWithIndex(PlayerTitleState state, TitleRegistry registry, TitleConditionIndex index) {
        List<Integer> unlocked = new ArrayList<>();
        java.util.Map<String, Integer> killCounts = state.getKillCounts();
        int aliveMinutes = state.getAliveMinutes();
        long bounty = 0L; // TODO: 从状态中获取bounty值
        
        // 使用索引获取所有可能需要检查的称号
        // 这里我们仍然需要检查所有称号，因为玩家状态可能有多个变化
        // 但我们可以使用索引来优化频繁检查
        // 对于定期检查，我们可以使用全量检查
        // 对于事件驱动检查（如击杀），调用更具体的方法
        
        // 这里保持全量检查以保持正确性，但使用索引的数据结构
        for (TitleConditionIndex.IndexEntry entry : index.getAllEntries()) {
            TitleDefinition definition = entry.getDefinition();
            
            if (state.isTitleUnlocked(definition.getId())) {
                continue;
            }

            if (definition.areAllConditionsMet(killCounts, java.util.Map.of(), aliveMinutes, bounty)) {
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
    
    private ProgressUpdateResult evaluateUnlocksLegacy(PlayerTitleState state, TitleRegistry registry) {
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
    
    private ProgressUpdateResult evaluateUnlocksForKill(PlayerTitleState state, TitleRegistry registry, 
                                                       TitleConditionIndex index, String entityId) {
        List<Integer> unlocked = new ArrayList<>();
        java.util.Map<String, Integer> killCounts = state.getKillCounts();
        int aliveMinutes = state.getAliveMinutes();
        long bounty = 0L;
        
        // 检查与该生物相关的称号
        List<TitleConditionIndex.IndexEntry> mobEntries = index.getTitlesForMobKill(entityId);
        // 检查任意敌对生物击杀相关的称号
        List<TitleConditionIndex.IndexEntry> anyHostileEntries = index.getTitlesForAnyHostileKill();
        
        // 合并需要检查的条目，避免重复检查
        java.util.Set<Integer> checkedTitleIds = new java.util.HashSet<>();
        
        checkEntries(unlocked, state, registry, mobEntries, killCounts, aliveMinutes, bounty, checkedTitleIds);
        checkEntries(unlocked, state, registry, anyHostileEntries, killCounts, aliveMinutes, bounty, checkedTitleIds);
        
        return unlocked.isEmpty() ? ProgressUpdateResult.empty() : new ProgressUpdateResult(List.copyOf(unlocked));
    }
    
    private void checkEntries(List<Integer> unlocked, PlayerTitleState state, TitleRegistry registry,
                             List<TitleConditionIndex.IndexEntry> entries,
                             java.util.Map<String, Integer> killCounts, int aliveMinutes, long bounty,
                             java.util.Set<Integer> checkedTitleIds) {
        for (TitleConditionIndex.IndexEntry entry : entries) {
            TitleDefinition definition = entry.getDefinition();
            int titleId = definition.getId();
            
            // 避免重复检查同一个称号
            if (checkedTitleIds.contains(titleId)) {
                continue;
            }
            checkedTitleIds.add(titleId);
            
            if (state.isTitleUnlocked(titleId)) {
                continue;
            }

            if (definition.areAllConditionsMet(killCounts, java.util.Map.of(), aliveMinutes, bounty)) {
                state.unlockTitle(titleId);
                unlocked.add(titleId);
                
                // 发布解锁事件
                try {
                    var event = eventFactory.createUnlockEvent(
                        state.getPlayerId(), titleId, Instant.now()
                    );
                    eventBus.publish(event);
                } catch (Exception e) {
                    // 事件发布失败不应影响解锁操作，但应记录日志
                    System.err.println("Failed to publish unlock event for title " + titleId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
         }
     }
     
     private ProgressUpdateResult evaluateUnlocksForSurvivalTime(PlayerTitleState state, TitleRegistry registry,
                                                                TitleConditionIndex index) {
         List<Integer> unlocked = new ArrayList<>();
         java.util.Map<String, Integer> killCounts = state.getKillCounts();
         int aliveMinutes = state.getAliveMinutes();
         long bounty = 0L;
         
         // 检查与生存时间相关的称号
         List<TitleConditionIndex.IndexEntry> survivalEntries = index.getTitlesForSurvivalTime();
         
         // 使用已有的checkEntries方法
         java.util.Set<Integer> checkedTitleIds = new java.util.HashSet<>();
         checkEntries(unlocked, state, registry, survivalEntries, killCounts, aliveMinutes, bounty, checkedTitleIds);
         
         return unlocked.isEmpty() ? ProgressUpdateResult.empty() : new ProgressUpdateResult(List.copyOf(unlocked));
     }
}