package com.kavinshi.playertitle.velocity.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final HikariDataSource dataSource;

    public DatabaseManager(com.kavinshi.playertitle.velocity.BridgeConfig bridgeConfig, java.nio.file.Path dataDirectory) {
        HikariConfig config = new HikariConfig();
        
        if (bridgeConfig.isUseMysql()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("MySQL JDBC Driver not found in classpath!", e);
            }
            
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                bridgeConfig.getMysqlHost(),
                bridgeConfig.getMysqlPort(),
                bridgeConfig.getMysqlDatabase()
            );
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(bridgeConfig.getMysqlUsername());
            config.setPassword(bridgeConfig.getMysqlPassword());
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(bridgeConfig.getMysqlTimeout());
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
        } else {
            try {
                Class.forName("org.sqlite.JDBC");
                config.setDriverClassName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("SQLite JDBC Driver not found in classpath!", e);
            }
            
            java.nio.file.Path dbFile = dataDirectory.resolve("database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath().toString());
            config.setMaximumPoolSize(1); // SQLite only supports 1 writer
            config.setConnectionTimeout(bridgeConfig.getMysqlTimeout());
        }

        this.dataSource = new HikariDataSource(config);
        initSchema(bridgeConfig.isUseIndexes(), bridgeConfig.isUseMysql());
    }

    private void initSchema(boolean useIndexes, boolean isMysql) {
        String autoIncrement = isMysql ? "BIGINT AUTO_INCREMENT" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String schema = "CREATE TABLE IF NOT EXISTS heading_audit_log (" +
            (isMysql ? "id " + autoIncrement + " PRIMARY KEY," : "id " + autoIncrement + ",") +
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
            if (useIndexes) {
                try {
                    stmt.execute("CREATE INDEX idx_audit_target ON heading_audit_log(target_uuid)");
                } catch (SQLException ignored) {
                    // Index might already exist
                }
            }
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
