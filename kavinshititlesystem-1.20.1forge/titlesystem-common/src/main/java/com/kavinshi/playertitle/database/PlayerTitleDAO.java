package com.kavinshi.playertitle.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.UUID;

public class PlayerTitleDAO {
    private final DatabaseManager dbManager;

    public PlayerTitleDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Update core attributes using optimistic locking
     */
    public void updatePlayerTitleCore(UUID uuid, int equippedId, int aliveMinutes, int currentVersion) throws SQLException {
        String sql = "UPDATE player_titles_core SET equipped_id = ?, alive_minutes = ?, version = version + 1 WHERE uuid = ? AND version = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, equippedId);
            pstmt.setInt(2, aliveMinutes);
            pstmt.setString(3, uuid.toString());
            pstmt.setInt(4, currentVersion);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new ConcurrentModificationException("Optimistic lock failed for player " + uuid + " at version " + currentVersion);
            }
        }
    }

    /**
     * Insert initial record if not exists
     */
    public void initPlayerCoreIfNotExists(UUID uuid) throws SQLException {
        // Check first for better cross-database compatibility without relying on INSERT IGNORE / INSERT OR IGNORE differences
        String checkSql = "SELECT 1 FROM player_titles_core WHERE uuid = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, uuid.toString());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return; // Record exists
                }
            }
        }
        
        String insertSql = "INSERT INTO player_titles_core (uuid, equipped_id, alive_minutes, version) VALUES (?, -1, 0, 1)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, uuid.toString());
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            // Ignore unique constraint violations if another thread inserted concurrently
            if (!e.getMessage().toLowerCase().contains("unique") && !e.getMessage().toLowerCase().contains("duplicate")) {
                throw e;
            }
        }
    }
}
