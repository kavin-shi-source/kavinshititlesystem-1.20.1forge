package com.kavinshi.playertitle.title;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标题注册表，负责管理所有标题定义并提供高效的查询功能。
 * 支持按条件索引查找符合条件的标题定义。
 */
public final class TitleRegistry {
    private final Map<Integer, TitleDefinition> titles = new ConcurrentHashMap<>();
    private TitleConditionIndex conditionIndex;

    public void loadAll(List<TitleDefinition> definitions) {
        this.titles.clear();
        for (TitleDefinition definition : definitions) {
            this.titles.put(definition.getId(), definition);
        }
        // 构建条件索引
        this.conditionIndex = TitleConditionIndex.build(this);
    }

    public TitleDefinition getTitle(int id) {
        return this.titles.get(id);
    }

    public List<TitleDefinition> getAllTitlesSorted() {
        List<TitleDefinition> sorted = new ArrayList<>(this.titles.values());
        sorted.sort(Comparator
            .comparingInt(TitleDefinition::getDisplayOrder)
            .thenComparingInt(TitleDefinition::getId));
        return sorted;
    }

    public int size() {
        return this.titles.size();
    }
    
    /**
     * 获取条件索引。
     * 
     * @return 条件索引，如果未构建则返回null
     */
    public TitleConditionIndex getConditionIndex() {
        return conditionIndex;
    }
    
    /**
     * 获取可能因击杀生物而解锁的称号定义。
     * 
     * @param entityId 生物ID
     * @return 可能解锁的称号定义列表
     */
    public List<TitleDefinition> getTitlesForMobKill(String entityId) {
        if (conditionIndex == null) {
            return List.of();
        }
        List<TitleConditionIndex.IndexEntry> entries = conditionIndex.getTitlesForMobKill(entityId);
        List<TitleDefinition> definitions = new ArrayList<>(entries.size());
        for (TitleConditionIndex.IndexEntry entry : entries) {
            definitions.add(entry.getDefinition());
        }
        return definitions;
    }
    
    /**
     * 获取可能因击杀任意敌对生物而解锁的称号定义。
     * 
     * @return 可能解锁的称号定义列表
     */
    public List<TitleDefinition> getTitlesForAnyHostileKill() {
        if (conditionIndex == null) {
            return List.of();
        }
        List<TitleConditionIndex.IndexEntry> entries = conditionIndex.getTitlesForAnyHostileKill();
        List<TitleDefinition> definitions = new ArrayList<>(entries.size());
        for (TitleConditionIndex.IndexEntry entry : entries) {
            definitions.add(entry.getDefinition());
        }
        return definitions;
    }
    
    /**
     * 获取可能因生存时间而解锁的称号定义。
     * 
     * @return 可能解锁的称号定义列表
     */
    public List<TitleDefinition> getTitlesForSurvivalTime() {
        if (conditionIndex == null) {
            return List.of();
        }
        List<TitleConditionIndex.IndexEntry> entries = conditionIndex.getTitlesForSurvivalTime();
        List<TitleDefinition> definitions = new ArrayList<>(entries.size());
        for (TitleConditionIndex.IndexEntry entry : entries) {
            definitions.add(entry.getDefinition());
        }
        return definitions;
    }
    
    /**
     * 获取可能因悬赏而解锁的称号定义。
     * 
     * @return 可能解锁的称号定义列表
     */
    public List<TitleDefinition> getTitlesForBounty() {
        if (conditionIndex == null) {
            return List.of();
        }
        List<TitleConditionIndex.IndexEntry> entries = conditionIndex.getTitlesForBounty();
        List<TitleDefinition> definitions = new ArrayList<>(entries.size());
        for (TitleConditionIndex.IndexEntry entry : entries) {
            definitions.add(entry.getDefinition());
        }
        return definitions;
    }
    
    /**
     * 获取索引统计信息。
     * 
     * @return 统计信息映射，如果索引未构建则返回空映射
     */
    public Map<String, Integer> getIndexStats() {
        if (conditionIndex == null) {
            return Map.of();
        }
        return conditionIndex.getStats();
    }
}
