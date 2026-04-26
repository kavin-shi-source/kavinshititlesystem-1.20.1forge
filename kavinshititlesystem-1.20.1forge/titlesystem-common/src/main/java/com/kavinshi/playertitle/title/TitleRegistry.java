package com.kavinshi.playertitle.title;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TitleRegistry {
    private volatile RegistrySnapshot snapshot = RegistrySnapshot.EMPTY;

    public void loadAll(List<TitleDefinition> definitions) {
        Map<Integer, TitleDefinition> newMap = new HashMap<>(definitions.size());
        for (TitleDefinition definition : definitions) {
            newMap.put(definition.getId(), definition);
        }
        Map<Integer, TitleDefinition> immutableMap = Map.copyOf(newMap);
        TitleConditionIndex index = TitleConditionIndex.buildFromMap(immutableMap);
        this.snapshot = new RegistrySnapshot(immutableMap, index);
    }

    public TitleDefinition getTitle(int id) {
        return snapshot.titles.get(id);
    }

    public List<TitleDefinition> getAllTitlesSorted() {
        List<TitleDefinition> sorted = new ArrayList<>(snapshot.titles.values());
        sorted.sort(Comparator
            .comparingInt(TitleDefinition::getDisplayOrder)
            .thenComparingInt(TitleDefinition::getId));
        return sorted;
    }

    public Map<Integer, TitleDefinition> getTitleMap() {
        return snapshot.titles;
    }

    public int size() {
        return snapshot.titles.size();
    }

    public TitleConditionIndex getConditionIndex() {
        return snapshot.conditionIndex;
    }

    public List<TitleDefinition> getTitlesForMobKill(String entityId) {
        List<TitleConditionIndex.IndexEntry> entries = snapshot.conditionIndex.getTitlesForMobKill(entityId);
        List<TitleDefinition> definitions = new ArrayList<>(entries.size());
        for (TitleConditionIndex.IndexEntry entry : entries) {
            definitions.add(entry.getDefinition());
        }
        return definitions;
    }

    public List<TitleDefinition> getTitlesForAnyHostileKill() {
        List<TitleConditionIndex.IndexEntry> entries = snapshot.conditionIndex.getTitlesForAnyHostileKill();
        List<TitleDefinition> definitions = new ArrayList<>(entries.size());
        for (TitleConditionIndex.IndexEntry entry : entries) {
            definitions.add(entry.getDefinition());
        }
        return definitions;
    }

    public List<TitleDefinition> getTitlesForSurvivalTime() {
        List<TitleConditionIndex.IndexEntry> entries = snapshot.conditionIndex.getTitlesForSurvivalTime();
        List<TitleDefinition> definitions = new ArrayList<>(entries.size());
        for (TitleConditionIndex.IndexEntry entry : entries) {
            definitions.add(entry.getDefinition());
        }
        return definitions;
    }

    public List<TitleDefinition> getTitlesForBounty() {
        List<TitleConditionIndex.IndexEntry> entries = snapshot.conditionIndex.getTitlesForBounty();
        List<TitleDefinition> definitions = new ArrayList<>(entries.size());
        for (TitleConditionIndex.IndexEntry entry : entries) {
            definitions.add(entry.getDefinition());
        }
        return definitions;
    }

    public Map<String, Integer> getIndexStats() {
        return snapshot.conditionIndex.getStats();
    }

    private static final class RegistrySnapshot {
        static final RegistrySnapshot EMPTY = new RegistrySnapshot(Map.of(), TitleConditionIndex.buildFromMap(Map.of()));

        final Map<Integer, TitleDefinition> titles;
        final TitleConditionIndex conditionIndex;

        RegistrySnapshot(Map<Integer, TitleDefinition> titles, TitleConditionIndex conditionIndex) {
            this.titles = titles;
            this.conditionIndex = conditionIndex;
        }
    }
}
