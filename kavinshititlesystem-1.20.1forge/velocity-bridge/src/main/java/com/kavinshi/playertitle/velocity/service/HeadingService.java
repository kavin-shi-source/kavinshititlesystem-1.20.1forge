package com.kavinshi.playertitle.velocity.service;

import com.kavinshi.playertitle.velocity.TitleCache;
import com.kavinshi.playertitle.velocity.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HeadingService {

    private final DatabaseManager dbManager;
    private final TitleCache titleCache;

    public HeadingService(DatabaseManager dbManager, TitleCache titleCache) {
        this.dbManager = dbManager;
        this.titleCache = titleCache;
    }

    public CompletableFuture<Boolean> grantHeading(UUID adminId, UUID targetId, String heading) {
        return CompletableFuture.supplyAsync(() -> {
            String oldHeading = "";
            Optional<TitleCache.TitleEntry> entryOpt = titleCache.get(targetId);
            if (entryOpt.isPresent()) {
                oldHeading = entryOpt.get().getHeading();
            }

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO heading_audit_log (admin_id, target_uuid, action, old_value, new_value, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
                 )) {
                stmt.setString(1, adminId.toString());
                stmt.setString(2, targetId.toString());
                stmt.setString(3, "GRANT");
                stmt.setString(4, oldHeading);
                stmt.setString(5, heading);
                stmt.setLong(6, System.currentTimeMillis());
                stmt.executeUpdate();

                // Update cache directly
                if (entryOpt.isPresent()) {
                    TitleCache.TitleEntry old = entryOpt.get();
                    titleCache.update(new TitleCache.TitleEntry(
                        old.getPlayerId(), old.getPlayerName(), old.getServerName(), old.getEquippedTitleId(),
                        old.getTitleName(), old.getTitleColor(), heading
                    ));
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> revokeHeading(UUID adminId, UUID targetId) {
        return grantHeading(adminId, targetId, "");
    }
}