package com.kavinshi.playertitle.title;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TitleConditionIndex {
    private volatile IndexSnapshot snapshot = IndexSnapshot.EMPTY;

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

    private static final class IndexSnapshot {
        static final IndexSnapshot EMPTY = new IndexSnapshot(
            Map.of(), Map.of(), Map.of(), Map.of());

        final Map<TitleConditionType, List<IndexEntry>> typeIndex;
        final Map<String, List<IndexEntry>> mobKillIndex;
        final Map<String, List<IndexEntry>> uuidKillIndex;
        final Map<Integer, List<IndexEntry>> allEntries;

        IndexSnapshot(Map<TitleConditionType, List<IndexEntry>> typeIndex,
                      Map<String, List<IndexEntry>> mobKillIndex,
                      Map<String, List<IndexEntry>> uuidKillIndex,
                      Map<Integer, List<IndexEntry>> allEntries) {
            this.typeIndex = typeIndex;
            this.mobKillIndex = mobKillIndex;
            this.uuidKillIndex = uuidKillIndex;
            this.allEntries = allEntries;
        }
    }

    public static TitleConditionIndex build(TitleRegistry registry) {
        return buildFromMap(registry.getTitleMap());
    }

    public static TitleConditionIndex buildFromMap(Map<Integer, TitleDefinition> titles) {
        TitleConditionIndex index = new TitleConditionIndex();
        List<TitleDefinition> sorted = new ArrayList<>(titles.values());
        sorted.sort(java.util.Comparator
            .comparingInt(TitleDefinition::getDisplayOrder)
            .thenComparingInt(TitleDefinition::getId));
        index.rebuildFromList(sorted);
        return index;
    }

    private void rebuildFromList(List<TitleDefinition> definitions) {
        Map<TitleConditionType, List<IndexEntry>> newTypeIndex = new HashMap<>();
        Map<String, List<IndexEntry>> newMobKillIndex = new HashMap<>();
        Map<String, List<IndexEntry>> newUuidKillIndex = new HashMap<>();
        Map<Integer, List<IndexEntry>> newAllEntries = new HashMap<>();

        for (TitleDefinition definition : definitions) {
            List<TitleCondition> conditions = definition.getConditions();
            if (conditions.isEmpty()) {
                continue;
            }

            for (int i = 0; i < conditions.size(); i++) {
                TitleCondition condition = conditions.get(i);
                IndexEntry entry = new IndexEntry(definition, condition, i);
                newAllEntries.computeIfAbsent(definition.getId(), k -> new ArrayList<>()).add(entry);

                switch (condition.getType()) {
                    case KILL_ANY_HOSTILE:
                    case SURVIVAL_TIME:
                    case BOUNTY:
                        newTypeIndex.computeIfAbsent(condition.getType(), k -> new ArrayList<>())
                                .add(entry);
                        break;

                    case KILL_MOB:
                        String mobKey = condition.getTarget().toLowerCase();
                        newMobKillIndex.computeIfAbsent(mobKey, k -> new ArrayList<>())
                                   .add(entry);
                        break;

                    case KILL_UUID:
                        String uuidKey = condition.getTarget().toLowerCase();
                        newUuidKillIndex.computeIfAbsent(uuidKey, k -> new ArrayList<>())
                                    .add(entry);
                        break;

                    default:
                        break;
                }
            }
        }

        Map<TitleConditionType, List<IndexEntry>> immutableTypeIndex = new HashMap<>(newTypeIndex.size());
        newTypeIndex.forEach((k, v) -> immutableTypeIndex.put(k, List.copyOf(v)));
        Map<String, List<IndexEntry>> immutableMobKillIndex = new HashMap<>(newMobKillIndex.size());
        newMobKillIndex.forEach((k, v) -> immutableMobKillIndex.put(k, List.copyOf(v)));
        Map<String, List<IndexEntry>> immutableUuidKillIndex = new HashMap<>(newUuidKillIndex.size());
        newUuidKillIndex.forEach((k, v) -> immutableUuidKillIndex.put(k, List.copyOf(v)));
        Map<Integer, List<IndexEntry>> immutableAllEntries = new HashMap<>(newAllEntries.size());
        newAllEntries.forEach((k, v) -> immutableAllEntries.put(k, List.copyOf(v)));

        this.snapshot = new IndexSnapshot(
            immutableTypeIndex, immutableMobKillIndex, immutableUuidKillIndex, immutableAllEntries);
    }

    public List<IndexEntry> getTitlesForMobKill(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return List.of();
        }
        String key = entityId.toLowerCase();
        List<IndexEntry> entries = snapshot.mobKillIndex.get(key);
        return entries != null ? entries : List.of();
    }

    public List<IndexEntry> getTitlesForAnyHostileKill() {
        List<IndexEntry> entries = snapshot.typeIndex.get(TitleConditionType.KILL_ANY_HOSTILE);
        return entries != null ? entries : List.of();
    }

    public List<IndexEntry> getTitlesForSurvivalTime() {
        List<IndexEntry> entries = snapshot.typeIndex.get(TitleConditionType.SURVIVAL_TIME);
        return entries != null ? entries : List.of();
    }

    public List<IndexEntry> getTitlesForBounty() {
        List<IndexEntry> entries = snapshot.typeIndex.get(TitleConditionType.BOUNTY);
        return entries != null ? entries : List.of();
    }

    public List<IndexEntry> getAllEntries() {
        List<IndexEntry> result = new ArrayList<>();
        for (List<IndexEntry> entries : snapshot.allEntries.values()) {
            result.addAll(entries);
        }
        return result;
    }

    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_entries", snapshot.allEntries.values().stream().mapToInt(List::size).sum());
        stats.put("kill_any_hostile", snapshot.typeIndex.getOrDefault(TitleConditionType.KILL_ANY_HOSTILE, List.of()).size());
        stats.put("survival_time", snapshot.typeIndex.getOrDefault(TitleConditionType.SURVIVAL_TIME, List.of()).size());
        stats.put("bounty", snapshot.typeIndex.getOrDefault(TitleConditionType.BOUNTY, List.of()).size());
        stats.put("unique_mobs", snapshot.mobKillIndex.size());
        stats.put("unique_uuids", snapshot.uuidKillIndex.size());

        int totalMobEntries = snapshot.mobKillIndex.values().stream().mapToInt(List::size).sum();
        int totalUuidEntries = snapshot.uuidKillIndex.values().stream().mapToInt(List::size).sum();
        stats.put("total_mob_entries", totalMobEntries);
        stats.put("total_uuid_entries", totalUuidEntries);

        return stats;
    }
}
