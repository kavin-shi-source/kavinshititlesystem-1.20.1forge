package com.kavinshi.playertitle.title;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 称号条件索引，用于优化称号解锁检查性能。
 * 根据条件类型和target对称号进行分组，避免遍历所有称号。
 */
public final class TitleConditionIndex {
    // 按条件类型分组：KILL_ANY_HOSTILE, SURVIVAL_TIME, BOUNTY
    private final Map<TitleConditionType, List<IndexEntry>> typeIndex = new ConcurrentHashMap<>();
    
    // 按生物类型分组的KILL_MOB条件
    private final Map<String, List<IndexEntry>> mobKillIndex = new ConcurrentHashMap<>();
    
    // 按UUID分组的KILL_UUID条件
    private final Map<String, List<IndexEntry>> uuidKillIndex = new ConcurrentHashMap<>();
    
    // 所有称号条目的映射
    private final Map<Integer, IndexEntry> allEntries = new ConcurrentHashMap<>();
    
    /**
     * 索引条目，包含称号定义和条件信息。
     */
    public static class IndexEntry {
        private final TitleDefinition definition;
        private final TitleCondition condition;
        private final int conditionIndex;
        
        public IndexEntry(TitleDefinition definition, TitleCondition condition, int conditionIndex) {
            this.definition = definition;
            this.condition = condition;
            this.conditionIndex = conditionIndex;
        }
        
        public TitleDefinition getDefinition() {
            return definition;
        }
        
        public TitleCondition getCondition() {
            return condition;
        }
        
        public int getConditionIndex() {
            return conditionIndex;
        }
    }
    
    /**
     * 构建称号条件索引。
     * 
     * @param registry 称号注册表
     * @return 构建好的索引
     */
    public static TitleConditionIndex build(TitleRegistry registry) {
        TitleConditionIndex index = new TitleConditionIndex();
        index.rebuild(registry);
        return index;
    }
    
    /**
     * 重建索引。
     * 
     * @param registry 称号注册表
     */
    public void rebuild(TitleRegistry registry) {
        clear();
        
        for (TitleDefinition definition : registry.getAllTitlesSorted()) {
            List<TitleCondition> conditions = definition.getConditions();
            if (conditions.isEmpty()) {
                continue;
            }
            
            // 为每个条件创建索引条目
            for (int i = 0; i < conditions.size(); i++) {
                TitleCondition condition = conditions.get(i);
                IndexEntry entry = new IndexEntry(definition, condition, i);
                allEntries.put(definition.getId(), entry);
                
                switch (condition.getType()) {
                    case KILL_ANY_HOSTILE:
                    case SURVIVAL_TIME:
                    case BOUNTY:
                        typeIndex.computeIfAbsent(condition.getType(), k -> new ArrayList<>())
                                .add(entry);
                        break;
                        
                    case KILL_MOB:
                        String mobKey = condition.getTarget().toLowerCase();
                        mobKillIndex.computeIfAbsent(mobKey, k -> new ArrayList<>())
                                   .add(entry);
                        break;
                        
                    case KILL_UUID:
                        String uuidKey = condition.getTarget().toLowerCase();
                        uuidKillIndex.computeIfAbsent(uuidKey, k -> new ArrayList<>())
                                    .add(entry);
                        break;
                }
            }
        }
    }
    
    /**
     * 清空索引。
     */
    public void clear() {
        typeIndex.clear();
        mobKillIndex.clear();
        uuidKillIndex.clear();
        allEntries.clear();
    }
    
    /**
     * 获取可能因击杀生物而解锁的称号。
     * 
     * @param entityId 生物ID
     * @return 可能解锁的称号条目列表
     */
    public List<IndexEntry> getTitlesForMobKill(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return List.of();
        }
        String key = entityId.toLowerCase();
        List<IndexEntry> entries = mobKillIndex.get(key);
        return entries != null ? new ArrayList<>(entries) : List.of();
    }
    
    /**
     * 获取可能因击杀任意敌对生物而解锁的称号。
     * 
     * @return 可能解锁的称号条目列表
     */
    public List<IndexEntry> getTitlesForAnyHostileKill() {
        List<IndexEntry> entries = typeIndex.get(TitleConditionType.KILL_ANY_HOSTILE);
        return entries != null ? new ArrayList<>(entries) : List.of();
    }
    
    /**
     * 获取可能因生存时间而解锁的称号。
     * 
     * @return 可能解锁的称号条目列表
     */
    public List<IndexEntry> getTitlesForSurvivalTime() {
        List<IndexEntry> entries = typeIndex.get(TitleConditionType.SURVIVAL_TIME);
        return entries != null ? new ArrayList<>(entries) : List.of();
    }
    
    /**
     * 获取可能因悬赏而解锁的称号。
     * 
     * @return 可能解锁的称号条目列表
     */
    public List<IndexEntry> getTitlesForBounty() {
        List<IndexEntry> entries = typeIndex.get(TitleConditionType.BOUNTY);
        return entries != null ? new ArrayList<>(entries) : List.of();
    }
    
    /**
     * 获取所有称号条目（用于全量检查）。
     * 
     * @return 所有称号条目列表
     */
    public List<IndexEntry> getAllEntries() {
        return new ArrayList<>(allEntries.values());
    }
    
    /**
     * 获取索引统计信息。
     * 
     * @return 统计信息映射
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_entries", allEntries.size());
        stats.put("kill_any_hostile", typeIndex.getOrDefault(TitleConditionType.KILL_ANY_HOSTILE, List.of()).size());
        stats.put("survival_time", typeIndex.getOrDefault(TitleConditionType.SURVIVAL_TIME, List.of()).size());
        stats.put("bounty", typeIndex.getOrDefault(TitleConditionType.BOUNTY, List.of()).size());
        stats.put("unique_mobs", mobKillIndex.size());
        stats.put("unique_uuids", uuidKillIndex.size());
        
        int totalMobEntries = mobKillIndex.values().stream().mapToInt(List::size).sum();
        int totalUuidEntries = uuidKillIndex.values().stream().mapToInt(List::size).sum();
        stats.put("total_mob_entries", totalMobEntries);
        stats.put("total_uuid_entries", totalUuidEntries);
        
        return stats;
    }
}