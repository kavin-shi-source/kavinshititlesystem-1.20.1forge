package com.kavinshi.playertitle.velocity;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

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

    public BridgeConfig(int serverNameColor) {
        this.serverNameColor = serverNameColor;
    }

    public int getServerNameColor() {
        return serverNameColor;
    }

    public static BridgeConfig load(Path dataDirectory, Logger logger) {
        int color = 0xAAAAAA;

        Path configFile = dataDirectory.resolve("config.properties");
        if (Files.notExists(configFile)) {
            try {
                Files.createDirectories(dataDirectory);
                try (InputStream in = BridgeConfig.class.getClassLoader().getResourceAsStream("default-config.properties")) {
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
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configFile)) {
                props.load(in);
                String colorName = props.getProperty("server-name-color", "gray").trim().toLowerCase();
                Integer hex = COLOR_HEX_MAP.get(colorName);
                if (hex != null) {
                    color = hex;
                    logger.info("Loaded server name color: {} ({})", colorName, String.format("#%06X", color));
                } else {
                    logger.warn("Unknown color name '{}', using default gray", colorName);
                }
            } catch (IOException e) {
                logger.warn("Failed to load config file, using defaults", e);
            }
        } else {
            logger.info("No config file found, using default server name color: gray");
        }

        return new BridgeConfig(color);
    }
}
