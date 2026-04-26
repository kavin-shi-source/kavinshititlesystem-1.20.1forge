package com.kavinshi.playertitle.velocity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TitleCache {

    private static final long ENTRY_TTL_MS = 30 * 60 * 1000L;
    private final Map<UUID, TitleEntry> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAccessTime = new ConcurrentHashMap<>();

    public void update(TitleEntry entry) {
        cache.put(entry.playerId, entry);
        lastAccessTime.put(entry.playerId, System.currentTimeMillis());
    }

    public void remove(UUID playerId) {
        cache.remove(playerId);
        lastAccessTime.remove(playerId);
    }

    public Optional<TitleEntry> get(UUID playerId) {
        TitleEntry entry = cache.get(playerId);
        if (entry != null) {
            lastAccessTime.put(playerId, System.currentTimeMillis());
        }
        return Optional.ofNullable(entry);
    }

    public boolean hasEntry(UUID playerId) {
        return cache.containsKey(playerId);
    }

    public Map<UUID, TitleEntry> getAll() {
        return Collections.unmodifiableMap(cache);
    }

    public void clear() {
        cache.clear();
        lastAccessTime.clear();
    }

    public int size() {
        return cache.size();
    }

    public int evictStaleEntries() {
        long now = System.currentTimeMillis();
        int evicted = 0;
        var iter = lastAccessTime.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (now - entry.getValue() > ENTRY_TTL_MS) {
                cache.remove(entry.getKey());
                iter.remove();
                evicted++;
            }
        }
        return evicted;
    }

    public static final class TitleEntry {
        private final UUID playerId;
        private final String playerName;
        private final int equippedTitleId;
        private final String titleName;
        private final int titleColor;
        private final String heading;
        private final String serverName;

        public TitleEntry(UUID playerId, String playerName, String serverName, int equippedTitleId,
                          String titleName, int titleColor, String heading) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.serverName = serverName;
            this.equippedTitleId = equippedTitleId;
            this.titleName = titleName;
            this.titleColor = titleColor;
            this.heading = heading;
        }

        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public int getEquippedTitleId() { return equippedTitleId; }
        public String getTitleName() { return titleName; }
        public int getTitleColor() { return titleColor; }
        public String getHeading() { return heading; }
        public String getServerName() { return serverName; }
        public boolean isHasTitle() { return equippedTitleId >= 0; }

        public String getEffectiveTitle() {
            if (equippedTitleId < 0) return "";
            return titleName != null ? titleName : "";
        }

        public int getEffectiveColor() {
            if (equippedTitleId < 0) return -1;
            return titleColor;
        }
    }
}
