package com.kavinshi.playertitle.client;

import com.kavinshi.playertitle.title.TitleDefinition;

import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientTitleData {
    private ClientTitleData() {}

    private static volatile PlayerDataSnapshot snapshot = PlayerDataSnapshot.EMPTY;
    private static final Map<UUID, EquippedTitleInfo> playerTitles = new ConcurrentHashMap<>();
    private static final Map<UUID, Component> componentCache = new ConcurrentHashMap<>();
    public static String heading = "";

    public static void updatePlayerData(int equippedId, Set<Integer> unlocked,
                                         Map<String, Integer> kills, int alive, String heading) {
        snapshot = new PlayerDataSnapshot(
            equippedId,
            unlocked != null ? Collections.unmodifiableSet(new HashSet<>(unlocked)) : Collections.emptySet(),
            kills != null ? Collections.unmodifiableMap(new HashMap<>(kills)) : Collections.emptyMap(),
            alive,
            snapshot.titleRegistry,
            snapshot.titleById
        );
        ClientTitleData.heading = heading != null ? heading : "";
    }

    public static void addUnlockedTitle(int titleId) {
        PlayerDataSnapshot current = snapshot;
        Set<Integer> newSet = new HashSet<>(current.unlockedTitles);
        newSet.add(titleId);
        snapshot = new PlayerDataSnapshot(
            current.equippedTitleId,
            Collections.unmodifiableSet(newSet),
            current.killCounts,
            current.aliveMinutes,
            current.titleRegistry,
            current.titleById
        );
    }

    public static void removeUnlockedTitle(int titleId) {
        PlayerDataSnapshot current = snapshot;
        Set<Integer> newSet = new HashSet<>(current.unlockedTitles);
        newSet.remove(titleId);
        snapshot = new PlayerDataSnapshot(
            current.equippedTitleId,
            Collections.unmodifiableSet(newSet),
            current.killCounts,
            current.aliveMinutes,
            current.titleRegistry,
            current.titleById
        );
    }

    public static void updateKillCount(String entityId, int count) {
        PlayerDataSnapshot current = snapshot;
        Map<String, Integer> newMap = new HashMap<>(current.killCounts);
        newMap.put(entityId, count);
        snapshot = new PlayerDataSnapshot(
            current.equippedTitleId,
            current.unlockedTitles,
            Collections.unmodifiableMap(newMap),
            current.aliveMinutes,
            current.titleRegistry,
            current.titleById
        );
    }

    public static void updateAliveMinutes(int minutes) {
        PlayerDataSnapshot current = snapshot;
        snapshot = new PlayerDataSnapshot(
            current.equippedTitleId,
            current.unlockedTitles,
            current.killCounts,
            minutes,
            current.titleRegistry,
            current.titleById
        );
    }

    public static void updateTitleRegistry(List<TitleDefinition> titles) {
        PlayerDataSnapshot current = snapshot;
        List<TitleDefinition> immutableList = Collections.unmodifiableList(new ArrayList<>(titles));
        Map<Integer, TitleDefinition> index = new HashMap<>(titles.size());
        for (TitleDefinition def : titles) {
            if (index.containsKey(def.getId())) {
                org.slf4j.LoggerFactory.getLogger(ClientTitleData.class).warn("Duplicate title ID: {}", def.getId());
            }
            index.put(def.getId(), def);
        }
        Map<Integer, TitleDefinition> immutableIndex = Collections.unmodifiableMap(index);
        snapshot = new PlayerDataSnapshot(
            current.equippedTitleId,
            current.unlockedTitles,
            current.killCounts,
            current.aliveMinutes,
            immutableList,
            immutableIndex
        );
    }

    public static void updatePlayerEquippedTitle(UUID playerId, int titleId,
                                                   String titleName, int titleColor, String chromaType,
                                                   int color2) {
        if (titleId >= 0) {
            EquippedTitleInfo info = new EquippedTitleInfo(titleId, titleName, titleColor,
                chromaType != null ? chromaType : "NONE", color2);
            playerTitles.put(playerId, info);
            
            // Build the static component to avoid GC storms during RenderNameTagEvent
            Component builtComp = com.kavinshi.playertitle.title.TitleDisplayHelper.createTitleComponent(
                info.titleName, info.titleColor, com.kavinshi.playertitle.title.ChromaType.fromString(info.chromaType), info.color2
            );
            componentCache.put(playerId, builtComp);
        } else {
            playerTitles.remove(playerId);
            componentCache.remove(playerId);
        }
    }

    public static Component getCachedTitleComponent(UUID playerId) {
        return componentCache.get(playerId);
    }

    public static void clearAll() {
        snapshot = PlayerDataSnapshot.EMPTY;
        playerTitles.clear();
        componentCache.clear();
    }

    public static int getEquippedTitleId() { return snapshot.equippedTitleId; }
    public static Set<Integer> getUnlockedTitles() { return snapshot.unlockedTitles; }
    public static Map<String, Integer> getKillCounts() { return snapshot.killCounts; }
    public static int getAliveMinutes() { return snapshot.aliveMinutes; }
    public static List<TitleDefinition> getTitleRegistry() { return snapshot.titleRegistry; }
    public static Map<UUID, EquippedTitleInfo> getPlayerTitles() { return playerTitles; }
    public static EquippedTitleInfo getEquippedTitleForPlayer(UUID playerId) { return playerTitles.get(playerId); }

    public static TitleDefinition getTitleById(int id) {
        return snapshot.titleById.get(id);
    }

    public static TitleDefinition getEquippedTitleDefinition() {
        PlayerDataSnapshot s = snapshot;
        if (s.equippedTitleId < 0) return null;
        return s.titleById.get(s.equippedTitleId);
    }

    private static final class PlayerDataSnapshot {
        static final PlayerDataSnapshot EMPTY = new PlayerDataSnapshot(
            -1, Collections.emptySet(), Collections.emptyMap(), 0,
            Collections.emptyList(), Collections.emptyMap()
        );

        final int equippedTitleId;
        final Set<Integer> unlockedTitles;
        final Map<String, Integer> killCounts;
        final int aliveMinutes;
        final List<TitleDefinition> titleRegistry;
        final Map<Integer, TitleDefinition> titleById;

        PlayerDataSnapshot(int equippedTitleId, Set<Integer> unlockedTitles,
                           Map<String, Integer> killCounts, int aliveMinutes,
                           List<TitleDefinition> titleRegistry,
                           Map<Integer, TitleDefinition> titleById) {
            this.equippedTitleId = equippedTitleId;
            this.unlockedTitles = unlockedTitles;
            this.killCounts = killCounts;
            this.aliveMinutes = aliveMinutes;
            this.titleRegistry = titleRegistry;
            this.titleById = titleById;
        }
    }

    public static final class EquippedTitleInfo {
        public final int titleId;
        public final String titleName;
        public final int titleColor;
        public final String chromaType;
        public final int color2;

        public EquippedTitleInfo(int titleId, String titleName, int titleColor, String chromaType, int color2) {
            this.titleId = titleId;
            this.titleName = titleName;
            this.titleColor = titleColor;
            this.chromaType = chromaType != null ? chromaType : "NONE";
            this.color2 = color2;
        }
    }
}
