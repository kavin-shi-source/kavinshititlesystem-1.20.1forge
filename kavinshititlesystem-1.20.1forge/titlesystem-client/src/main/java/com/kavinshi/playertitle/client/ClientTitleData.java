package com.kavinshi.playertitle.client;

import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.title.TitleDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientTitleData {
    private ClientTitleData() {}

    private static final Set<Integer> EMPTY_INT_SET = Collections.emptySet();
    private static final Map<String, Integer> EMPTY_STR_INT_MAP = Collections.emptyMap();

    private static int equippedTitleId = -1;
    private static Set<Integer> unlockedTitles = EMPTY_INT_SET;
    private static Map<String, Integer> killCounts = EMPTY_STR_INT_MAP;
    private static int aliveMinutes = 0;
    private static List<TitleDefinition> titleRegistry = Collections.emptyList();
    private static final Map<UUID, EquippedTitleInfo> playerTitles = new ConcurrentHashMap<>();
    private static CustomTitleData customTitle = new CustomTitleData();

    public static void updatePlayerData(int equippedId, Set<Integer> unlocked,
                                         Map<String, Integer> kills, int alive,
                                         CustomTitleData ct) {
        equippedTitleId = equippedId;
        unlockedTitles = unlocked;
        killCounts = kills;
        aliveMinutes = alive;
        if (ct != null) {
            customTitle = ct;
        }
    }

    public static void addUnlockedTitle(int titleId) {
        if (unlockedTitles == EMPTY_INT_SET) {
            unlockedTitles = new HashSet<>();
        }
        unlockedTitles.add(titleId);
    }

    public static void removeUnlockedTitle(int titleId) {
        if (unlockedTitles != EMPTY_INT_SET) {
            unlockedTitles.remove(titleId);
        }
    }

    public static void updateKillCount(String entityId, int count) {
        if (killCounts == EMPTY_STR_INT_MAP) {
            killCounts = new HashMap<>();
        }
        killCounts.put(entityId, count);
    }

    public static void updateAliveMinutes(int minutes) {
        aliveMinutes = minutes;
    }

    public static void updateTitleRegistry(List<TitleDefinition> titles) {
        titleRegistry = titles;
    }

    public static void updatePlayerEquippedTitle(UUID playerId, int titleId,
                                                   String titleName, int titleColor, String chromaType) {
        if (titleId >= 0) {
            playerTitles.put(playerId, new EquippedTitleInfo(titleId, titleName, titleColor,
                chromaType != null ? chromaType : "NONE"));
        } else {
            playerTitles.remove(playerId);
        }
    }

    public static void updatePlayerCustomTitle(UUID playerId, CustomTitleData ct) {
        if (ct != null && ct.isUsingCustomTitle() && ct.hasPermission()) {
            playerTitles.put(playerId, new EquippedTitleInfo(-2, ct.getText(),
                ct.getColor1(), ct.getEffectiveChromaType().name()));
        }
    }

    public static void clearAll() {
        equippedTitleId = -1;
        unlockedTitles = EMPTY_INT_SET;
        killCounts = EMPTY_STR_INT_MAP;
        aliveMinutes = 0;
        titleRegistry = Collections.emptyList();
        playerTitles.clear();
        customTitle = new CustomTitleData();
    }

    public static int getEquippedTitleId() { return equippedTitleId; }
    public static Set<Integer> getUnlockedTitles() { return unlockedTitles; }
    public static Map<String, Integer> getKillCounts() { return killCounts; }
    public static int getAliveMinutes() { return aliveMinutes; }
    public static List<TitleDefinition> getTitleRegistry() { return titleRegistry; }
    public static Map<UUID, EquippedTitleInfo> getPlayerTitles() { return playerTitles; }
    public static EquippedTitleInfo getEquippedTitleForPlayer(UUID playerId) { return playerTitles.get(playerId); }
    public static CustomTitleData getCustomTitle() { return customTitle; }

    public static TitleDefinition getEquippedTitleDefinition() {
        if (equippedTitleId < 0) return null;
        for (TitleDefinition def : titleRegistry) {
            if (def.getId() == equippedTitleId) return def;
        }
        return null;
    }

    public static boolean isUsingCustomTitle() {
        return customTitle.isUsingCustomTitle() && customTitle.hasPermission();
    }

    public static String getEffectiveTitleName() {
        if (isUsingCustomTitle()) return customTitle.getText();
        TitleDefinition def = getEquippedTitleDefinition();
        return def != null ? def.getName() : "";
    }

    public static int getEffectiveTitleColor() {
        if (isUsingCustomTitle()) return customTitle.getColor1();
        TitleDefinition def = getEquippedTitleDefinition();
        return def != null ? def.getColor() : 0xFFFFFF;
    }

    public static String getEffectiveChromaType() {
        if (isUsingCustomTitle()) return customTitle.getEffectiveChromaType().name();
        TitleDefinition def = getEquippedTitleDefinition();
        return def != null ? def.getChromaType() : "NONE";
    }

    public static final class EquippedTitleInfo {
        public final int titleId;
        public final String titleName;
        public final int titleColor;
        public final String chromaType;

        public EquippedTitleInfo(int titleId, String titleName, int titleColor, String chromaType) {
            this.titleId = titleId;
            this.titleName = titleName;
            this.titleColor = titleColor;
            this.chromaType = chromaType != null ? chromaType : "NONE";
        }
    }
}
