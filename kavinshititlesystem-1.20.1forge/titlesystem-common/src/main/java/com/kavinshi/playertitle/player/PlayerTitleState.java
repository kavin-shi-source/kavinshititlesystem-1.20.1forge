package com.kavinshi.playertitle.player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTitleState {
    private final UUID playerId;
    private final Set<Integer> unlockedTitles = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> killCounts = new ConcurrentHashMap<>();
    private volatile int equippedTitleId = -1;
    private volatile int aliveMinutes;
    private String heading = "";
    private boolean dirty = false;
    private volatile int version = 1;
    private volatile long lastLoadTime = 0;

    public PlayerTitleState(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public int getEquippedTitleId() {
        return this.equippedTitleId;
    }

    public void setEquippedTitleId(int equippedTitleId) {
        if (this.equippedTitleId != equippedTitleId) {
            this.equippedTitleId = equippedTitleId;
            this.dirty = true;
        }
    }

    public boolean isTitleUnlocked(int titleId) {
        return this.unlockedTitles.contains(titleId);
    }

    public Set<Integer> getUnlockedTitleIds() {
        return Set.copyOf(this.unlockedTitles);
    }

    public int getKillCount(String entityId) {
        return this.killCounts.getOrDefault(entityId.toLowerCase(), 0);
    }

    public Map<String, Integer> getKillCounts() {
        return Map.copyOf(this.killCounts);
    }

    public void setKillCounts(Map<String, Integer> newCounts) {
        this.killCounts.clear();
        for (var entry : newCounts.entrySet()) {
            this.killCounts.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        this.dirty = true;
    }

    public void addKill(String entityId) {
        String key = entityId.toLowerCase();
        this.killCounts.merge(key, 1, Integer::sum);
        this.dirty = true;
    }

    public void addKillCount(String entityId, int count) {
        if (count <= 0) return;
        String key = entityId.toLowerCase();
        this.killCounts.merge(key, count, Integer::sum);
        this.dirty = true;
    }

    public int getAliveMinutes() {
        return this.aliveMinutes;
    }

    public void setAliveMinutes(int aliveMinutes) {
        if (this.aliveMinutes != aliveMinutes) {
            this.aliveMinutes = aliveMinutes;
            this.dirty = true;
        }
    }

    public String getHeading() {
        return this.heading;
    }

    public void setHeading(String heading) {
        this.heading = heading != null ? heading : "";
        this.dirty = true;
    }

    public void unlockTitle(int titleId) {
        if (this.unlockedTitles.add(titleId)) {
            this.dirty = true;
        }
    }

    public void revokeTitle(int titleId) {
        if (this.unlockedTitles.remove(titleId)) {
            if (this.equippedTitleId == titleId) {
                this.equippedTitleId = -1;
            }
            this.dirty = true;
        }
    }

    public void setKillCountsSilently(Map<String, Integer> newCounts) {
        this.killCounts.clear();
        for (var entry : newCounts.entrySet()) {
            this.killCounts.put(entry.getKey().toLowerCase(), entry.getValue());
        }
    }

    public void unlockTitleSilently(int titleId) {
        this.unlockedTitles.add(titleId);
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getLastLoadTime() {
        return lastLoadTime;
    }

    public void updateLastLoadTime() {
        this.lastLoadTime = System.currentTimeMillis();
    }

    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - this.lastLoadTime > ttlMillis;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markClean() {
        this.dirty = false;
    }
}
