package com.kavinshi.playertitle.database;

import com.kavinshi.playertitle.player.PlayerTitleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ConcurrentModificationException;

public class PlayerTitleRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerTitleRepository.class);
    private final DatabaseManager dbManager;

    public PlayerTitleRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public PlayerTitleState loadPlayerState(UUID uuid) throws SQLException {
        PlayerTitleState state = new PlayerTitleState(uuid);
        
        // Ensure core record exists
        new PlayerTitleDAO(dbManager).initPlayerCoreIfNotExists(uuid);

        try (Connection conn = dbManager.getConnection()) {
            // Load core
            try (PreparedStatement stmt = conn.prepareStatement("SELECT equipped_id, alive_minutes, version FROM player_titles_core WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        state.setEquippedTitleId(rs.getInt("equipped_id"));
                        state.setAliveMinutes(rs.getInt("alive_minutes"));
                        state.setVersion(rs.getInt("version"));
                    }
                }
            }

            // Load unlocked titles
            try (PreparedStatement stmt = conn.prepareStatement("SELECT title_id FROM player_unlocked_titles WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        state.unlockTitleSilently(rs.getInt("title_id"));
                    }
                }
            }

            // Load kill counts
            try (PreparedStatement stmt = conn.prepareStatement("SELECT target_type, kill_count FROM player_kill_counts WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    Map<String, Integer> kills = new HashMap<>();
                    while (rs.next()) {
                        kills.put(rs.getString("target_type"), rs.getInt("kill_count"));
                    }
                    state.setKillCountsSilently(kills);
                }
            }
        }
        
        state.updateLastLoadTime();
        state.markClean();
        return state;
    }

    public void savePlayerState(PlayerTitleState state) throws SQLException {
        if (!state.isDirty()) return;

        UUID uuid = state.getPlayerId();
        
        // Ensure core record exists before saving, in case it was created and modified before initial load completed
        new PlayerTitleDAO(dbManager).initPlayerCoreIfNotExists(uuid);

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update core with optimistic locking
                int currentVersion = state.getVersion();
                String updateCore = "UPDATE player_titles_core SET equipped_id = ?, alive_minutes = ?, version = version + 1 WHERE uuid = ? AND version = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateCore)) {
                    stmt.setInt(1, state.getEquippedTitleId());
                    stmt.setInt(2, state.getAliveMinutes());
                    stmt.setString(3, uuid.toString());
                    stmt.setInt(4, currentVersion);
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        throw new ConcurrentModificationException("Optimistic lock failed for player " + uuid + " at version " + currentVersion);
                    }
                }
                
                state.setVersion(currentVersion + 1);

                // Update unlocked titles
                // We can just clear and re-insert, or merge. Clearing is simpler but might be slightly slower. 
                // Let's do clear and insert since the set is small.
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM player_unlocked_titles WHERE uuid = ?")) {
                    stmt.setString(1, uuid.toString());
                    stmt.executeUpdate();
                }
                
                Set<Integer> unlocked = state.getUnlockedTitleIds();
                if (!unlocked.isEmpty()) {
                    try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO player_unlocked_titles (uuid, title_id) VALUES (?, ?)")) {
                        for (int titleId : unlocked) {
                            stmt.setString(1, uuid.toString());
                            stmt.setInt(2, titleId);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                // Update kill counts
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM player_kill_counts WHERE uuid = ?")) {
                    stmt.setString(1, uuid.toString());
                    stmt.executeUpdate();
                }
                
                Map<String, Integer> kills = state.getKillCounts();
                if (!kills.isEmpty()) {
                    try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO player_kill_counts (uuid, target_type, kill_count) VALUES (?, ?, ?)")) {
                        for (Map.Entry<String, Integer> entry : kills.entrySet()) {
                            stmt.setString(1, uuid.toString());
                            stmt.setString(2, entry.getKey());
                            stmt.setInt(3, entry.getValue());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                conn.commit();
                state.markClean();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
