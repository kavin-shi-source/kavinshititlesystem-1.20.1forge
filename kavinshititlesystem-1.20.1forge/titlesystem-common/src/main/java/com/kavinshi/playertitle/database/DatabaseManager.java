package com.kavinshi.playertitle.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private HikariDataSource dataSource;
    private final DatabaseConfig config;

    public DatabaseManager(DatabaseConfig config) {
        this.config = config;
    }

    public void init() {
        initPool();
        initSchema();
    }

    private void initPool() {
        HikariConfig hc = new HikariConfig();
        
        if ("cluster".equalsIgnoreCase(config.getServerMode())) {
            // MySQL
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                    config.getHost(), config.getPort(), config.getDatabase());
            hc.setJdbcUrl(jdbcUrl);
            hc.setUsername(config.getUsername());
            hc.setPassword(config.getPassword());
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                hc.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                LOGGER.error("MySQL JDBC Driver not found in classpath!", e);
            }
            
            hc.setMaximumPoolSize(config.getPoolSize());
            hc.setConnectionTimeout(config.getTimeout());
            
            try {
                this.dataSource = new HikariDataSource(hc);
                try (Connection conn = this.dataSource.getConnection()) {
                    LOGGER.info("Successfully connected to MySQL database in cluster mode.");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to connect to MySQL database! Server cannot start in cluster mode.", e);
                if (this.dataSource != null) {
                    this.dataSource.close();
                }
                throw new IllegalStateException("Database connection failed in cluster mode", e);
            }
        } else {
            // SQLite
            hc.setJdbcUrl("jdbc:sqlite:playertitle.db");
            try {
                Class.forName("org.sqlite.JDBC");
                hc.setDriverClassName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                LOGGER.error("SQLite JDBC Driver not found in classpath!", e);
            }
            hc.setMaximumPoolSize(1); // SQLite doesn't need a large pool
            hc.setConnectionTimeout(config.getTimeout());
            this.dataSource = new HikariDataSource(hc);
            LOGGER.info("Database connection pool initialized for mode: single (SQLite)");
        }
    }

    private void initSchema() {
        String schemaFile = "cluster".equalsIgnoreCase(config.getServerMode()) ? "/sql/schema_mysql.sql" : "/sql/schema_sqlite.sql";
        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (InputStream is = getClass().getResourceAsStream(schemaFile);
                 Statement stmt = conn.createStatement()) {
                
                if (is == null) {
                    LOGGER.error("Schema file not found: {}", schemaFile);
                    conn.setAutoCommit(originalAutoCommit);
                    return;
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sqlBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                    sqlBuilder.append(line).append(" ");
                    if (trimmed.endsWith(";")) {
                        stmt.execute(sqlBuilder.toString());
                        sqlBuilder.setLength(0); // Reset for next statement
                    }
                }
                conn.commit();
                LOGGER.info("Database schema {} executed successfully.", schemaFile);
            } catch (Exception e) {
                conn.rollback();
                LOGGER.error("Failed to initialize database schema, transaction rolled back", e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception e) {
            LOGGER.error("Database schema initialization encountered an error", e);
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
