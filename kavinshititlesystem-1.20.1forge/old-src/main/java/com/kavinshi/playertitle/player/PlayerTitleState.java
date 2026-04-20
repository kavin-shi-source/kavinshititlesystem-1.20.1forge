package com.kavinshi.playertitle.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerTitleState {
    private final UUID playerId;
    private final Set<Integer> unlockedTitles = new HashSet<>();
    private final Map<String, Integer> killCounts = new HashMap<>();
    private int equippedTitleId = -1;
    private int aliveMinutes;
    private boolean dirty = true;

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

    public void addKill(String entityId) {
        String key = entityId.toLowerCase();
        this.killCounts.merge(key, 1, Integer::sum);
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

    public boolean isDirty() {
        return this.dirty;
    }

    public void markClean() {
        this.dirty = false;
    }
}
