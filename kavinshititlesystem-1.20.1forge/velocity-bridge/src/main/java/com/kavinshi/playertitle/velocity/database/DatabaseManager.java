package com.kavinshi.playertitle.velocity.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final HikariDataSource dataSource;

    public DatabaseManager(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
        initSchema();
    }

    private void initSchema() {
        String schema = "CREATE TABLE IF NOT EXISTS heading_audit_log (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "admin_id VARCHAR(36) NOT NULL," +
            "target_uuid VARCHAR(36) NOT NULL," +
            "action VARCHAR(16) NOT NULL," +
            "old_value VARCHAR(255)," +
            "new_value VARCHAR(255)," +
            "timestamp BIGINT NOT NULL" +
            ");";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(schema);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}