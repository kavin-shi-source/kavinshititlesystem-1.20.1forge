package com.kavinshi.playertitle.velocity;

import com.moandjiezana.toml.Toml;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class BridgeConfig {

    private static final Map<String, Integer> COLOR_HEX_MAP = Map.ofEntries(
        Map.entry("black", 0x000000),
        Map.entry("dark_blue", 0x0000AA),
        Map.entry("dark_green", 0x00AA00),
        Map.entry("dark_aqua", 0x00AAAA),
        Map.entry("dark_red", 0xAA0000),
        Map.entry("dark_purple", 0xAA00AA),
        Map.entry("gold", 0xFFAA00),
        Map.entry("gray", 0xAAAAAA),
        Map.entry("dark_gray", 0x555555),
        Map.entry("blue", 0x5555FF),
        Map.entry("green", 0x55FF55),
        Map.entry("aqua", 0x55FFFF),
        Map.entry("red", 0xFF5555),
        Map.entry("light_purple", 0xFF55FF),
        Map.entry("yellow", 0xFFFF55),
        Map.entry("white", 0xFFFFFF)
    );

    private final int serverNameColor;
    
    // Database config
    private final boolean useMysql;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final long mysqlTimeout;
    private final boolean useIndexes;

    public BridgeConfig(int serverNameColor, boolean useMysql, String mysqlHost, int mysqlPort, 
                        String mysqlDatabase, String mysqlUsername, String mysqlPassword, 
                        long mysqlTimeout, boolean useIndexes) {
        this.serverNameColor = serverNameColor;
        this.useMysql = useMysql;
        this.mysqlHost = mysqlHost;
        this.mysqlPort = mysqlPort;
        this.mysqlDatabase = mysqlDatabase;
        this.mysqlUsername = mysqlUsername;
        this.mysqlPassword = mysqlPassword;
        this.mysqlTimeout = mysqlTimeout;
        this.useIndexes = useIndexes;
    }

    public int getServerNameColor() { return serverNameColor; }
    public boolean isUseMysql() { return useMysql; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
    public long getMysqlTimeout() { return mysqlTimeout; }
    public boolean isUseIndexes() { return useIndexes; }

    public static BridgeConfig load(Path dataDirectory, Logger logger) {
        int color = 0xAAAAAA;
        
        // Defaults
        boolean useMysql = true;
        String mysqlHost = "localhost";
        int mysqlPort = 3306;
        String mysqlDatabase = "playertitle";
        String mysqlUsername = "root";
        String mysqlPassword = "";
        long mysqlTimeout = 5000L;
        boolean useIndexes = true;

        Path configFile = dataDirectory.resolve("config.toml");
        if (Files.notExists(configFile)) {
            try {
                Files.createDirectories(dataDirectory);
                try (InputStream in = BridgeConfig.class.getClassLoader().getResourceAsStream("default-config.toml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                        logger.info("Created default config file: {}", configFile);
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to create default config file", e);
            }
        }

        if (Files.exists(configFile)) {
            try {
                Toml toml = new Toml().read(configFile.toFile());
                String colorName = toml.getString("server-name-color", "gray").trim().toLowerCase();
                Integer hex = COLOR_HEX_MAP.get(colorName);
                if (hex != null) {
                    color = hex;
                    logger.info("Loaded server name color: {} ({})", colorName, String.format("#%06X", color));
                } else {
                    logger.warn("Unknown color name '{}', using default gray", colorName);
                }
                
                Toml dbToml = toml.getTable("database");
                if (dbToml != null) {
                    useMysql = dbToml.getBoolean("useMysql", true);
                    mysqlHost = dbToml.getString("mysqlHost", "localhost");
                    mysqlPort = dbToml.getLong("mysqlPort", 3306L).intValue();
                    mysqlDatabase = dbToml.getString("mysqlDatabase", "playertitle");
                    mysqlUsername = dbToml.getString("mysqlUsername", "root");
                    mysqlPassword = dbToml.getString("mysqlPassword", "");
                    mysqlTimeout = dbToml.getLong("mysqlTimeout", 5000L);
                    useIndexes = dbToml.getBoolean("useIndexes", true);
                }
            } catch (Exception e) {
                logger.warn("Failed to load config.toml, using defaults", e);
            }
        } else {
            logger.info("No config.toml file found, using default settings.");
        }

        return new BridgeConfig(color, useMysql, mysqlHost, mysqlPort, mysqlDatabase, mysqlUsername, mysqlPassword, mysqlTimeout, useIndexes);
    }
}
