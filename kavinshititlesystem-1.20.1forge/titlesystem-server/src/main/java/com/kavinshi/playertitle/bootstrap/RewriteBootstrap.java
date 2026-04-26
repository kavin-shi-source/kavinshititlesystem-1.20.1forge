package com.kavinshi.playertitle.bootstrap;

import com.kavinshi.playertitle.config.JsonTitleConfigRepository;
import com.kavinshi.playertitle.handler.PlayerTitleDataBridge;
import com.kavinshi.playertitle.icon.IconManager;
import com.kavinshi.playertitle.icon.IconService;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.sync.*;
import com.kavinshi.playertitle.service.TitleEquipService;
import com.kavinshi.playertitle.service.TitleProgressService;
import com.kavinshi.playertitle.title.TitleRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RewriteBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile RewriteBootstrap instance;

    private IconManager iconManager;
    private IconService iconService;
    private TitleRegistry titleRegistry;
    private ClusterRevisionService revisionService;
    private TitleEventFactory eventFactory;
    private TitleEquipService titleEquipService;
    private TitleProgressService titleProgressService;
    private ClusterEventBus eventBus;
    private ClusterConfig clusterConfig;
    private PlayerTitleDataBridge playerTitleDataBridge;

    private RewriteBootstrap() {
        initializeComponents();
    }

    public static RewriteBootstrap initialize() {
        if (instance == null) {
            instance = new RewriteBootstrap();
        }
        return instance;
    }

    public static RewriteBootstrap getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RewriteBootstrap has not been initialized");
        }
        return instance;
    }

    private void initializeComponents() {
        LOGGER.info("Initializing PlayerTitle components...");

        NetworkHandler.init();
        NetworkHandler.registerPackets();
        LOGGER.info("Network handler initialized");

        this.titleRegistry = loadTitleRegistry();
        LOGGER.info("Title registry loaded: {} definitions", titleRegistry.size());

        this.clusterConfig = loadClusterConfig();
        LOGGER.info("Cluster config loaded: mode={}", clusterConfig.getMode());

        Path configDir = Path.of("config", "playertitle", "icons");
        this.iconManager = new IconManager(configDir);
        try {
            this.iconManager.scanIcons();
            LOGGER.info("Icon manager initialized, loaded {} icons", this.iconManager.getIconCount());
        } catch (IOException e) {
            LOGGER.error("Failed to scan icons from directory: {}", configDir, e);
        }

        this.iconService = new IconService(this.iconManager);
        this.revisionService = new ClusterRevisionService();
        this.eventFactory = new TitleEventFactory(this.revisionService);

        this.eventBus = createEventBus(clusterConfig);
        try {
            this.eventBus.start();
            LOGGER.info("Event bus started: {}", this.eventBus.getImplementationName());
        } catch (Exception e) {
            LOGGER.error("Failed to start event bus: {}, falling back to LOCAL", e.getMessage());
            try {
                this.eventBus.stop();
            } catch (Exception stopEx) {
                LOGGER.warn("Failed to stop failed event bus", stopEx);
            }
            this.eventBus = new LocalEventBus();
        }

        this.titleEquipService = new TitleEquipService(this.eventFactory, this.eventBus);
        this.titleProgressService = new TitleProgressService(this.eventFactory, this.eventBus);
        LOGGER.info("PlayerTitle components initialized successfully");
    }

    private TitleRegistry loadTitleRegistry() {
        TitleRegistry registry = new TitleRegistry();
        Path configPath = Path.of("config", "playertitle", "titles.json");
        if (Files.exists(configPath)) {
            try {
                var definitions = new JsonTitleConfigRepository().loadDefinitions(configPath);
                registry.loadAll(definitions);
            } catch (Exception e) {
                LOGGER.error("Failed to load title definitions from {}", configPath, e);
            }
        } else {
            LOGGER.warn("Title config not found at {}, creating default config", configPath);
            try {
                Files.createDirectories(configPath.getParent());
                createDefaultTitlesConfig(configPath);
                var definitions = new JsonTitleConfigRepository().loadDefinitions(configPath);
                registry.loadAll(definitions);
                LOGGER.info("Created and loaded default title config with {} definitions", registry.size());
            } catch (IOException e) {
                LOGGER.error("Failed to create default config directory", e);
            } catch (Exception e) {
                LOGGER.error("Failed to load default title definitions", e);
            }
        }
        return registry;
    }

    private void createDefaultTitlesConfig(Path configPath) throws IOException {
        try (var is = RewriteBootstrap.class.getResourceAsStream("/default_titles.json")) {
            if (is != null) {
                Files.copy(is, configPath);
                LOGGER.info("Copied default titles.json to {}", configPath);
            } else {
                Files.writeString(configPath, "[]");
                LOGGER.warn("default_titles.json resource not found, created empty config");
            }
        }
    }

    private ClusterConfig loadClusterConfig() {
        Path configFile = Path.of("config", "playertitle", "cluster.json");
        if (Files.exists(configFile)) {
            try {
                String content = Files.readString(configFile);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(content, ClusterConfig.class);
            } catch (IOException e) {
                LOGGER.warn("Failed to load cluster config from {}, using defaults", configFile);
            }
        }
        return ClusterConfig.defaultConfig();
    }

    private ClusterEventBus createEventBus(ClusterConfig config) {
        return switch (config.getMode()) {
            case LOCAL -> new LocalEventBus();
            case REDIS -> new RedisEventBus(
                config.getRedisHost(),
                config.getRedisPort(),
                config.getRedisPassword(),
                config.getChannelName()
            );
            case VELOCITY -> new VelocityEventBus(
                config.getChannelName(),
                config.getServerName()
            );
        };
    }

    public IconManager getIconManager() { return iconManager; }
    public IconService getIconService() { return iconService; }
    public TitleRegistry getTitleRegistry() { return titleRegistry; }
    public ClusterRevisionService getRevisionService() { return revisionService; }
    public TitleEventFactory getEventFactory() { return eventFactory; }
    public TitleEquipService getTitleEquipService() { return titleEquipService; }
    public TitleProgressService getTitleProgressService() { return titleProgressService; }
    public ClusterEventBus getEventBus() { return eventBus; }
    public ClusterConfig getClusterConfig() { return clusterConfig; }

    public void onServerStarting(MinecraftServer server) {
        this.titleEquipService.onServerStarting(server);
        this.titleProgressService.onServerStarting(server);

        if (this.playerTitleDataBridge == null) {
            this.playerTitleDataBridge = new PlayerTitleDataBridge(server, titleRegistry, clusterConfig);
            this.playerTitleDataBridge.subscribeToEvents(eventBus);
            LOGGER.info("PlayerTitleDataBridge initialized and subscribed to events");
        }
    }

    public PlayerTitleDataBridge getPlayerTitleDataBridge() { return playerTitleDataBridge; }
}
