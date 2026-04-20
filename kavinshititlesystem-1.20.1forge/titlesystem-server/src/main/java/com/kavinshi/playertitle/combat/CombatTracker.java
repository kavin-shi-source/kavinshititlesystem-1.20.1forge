package com.kavinshi.playertitle.combat;

import com.kavinshi.playertitle.config.TitleConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatTracker {
    private static final Map<UUID, Long> combatTimestamps = new ConcurrentHashMap<>();
    private static long lastCleanup = System.currentTimeMillis();

    public static void markCombat(UUID playerId) {
        combatTimestamps.put(playerId, System.currentTimeMillis());
    }

    public static boolean isInCombat(UUID playerId) {
        Long timestamp = combatTimestamps.get(playerId);
        if (timestamp == null) return false;
        int durationSeconds = getCombatDuration();
        return (System.currentTimeMillis() - timestamp) < (durationSeconds * 1000L);
    }

    public static int getRemainingCombatSeconds(UUID playerId) {
        Long timestamp = combatTimestamps.get(playerId);
        if (timestamp == null) return 0;
        int durationSeconds = getCombatDuration();
        long remaining = (durationSeconds * 1000L) - (System.currentTimeMillis() - timestamp);
        return remaining > 0L ? (int) (remaining / 1000L) : 0;
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < 300000L) return;
        lastCleanup = now;
        int durationSeconds = getCombatDuration();
        long threshold = durationSeconds * 1000L;
        combatTimestamps.entrySet().removeIf(entry -> (now - entry.getValue()) > threshold);
    }

    private static int getCombatDuration() {
        try {
            return TitleConfig.SERVER.combatDuration.get();
        } catch (Exception e) {
            return 30;
        }
    }
}
