package com.kavinshi.playertitle.bootstrap;

import com.kavinshi.playertitle.ModConstants;
import com.kavinshi.playertitle.icon.IconManager;
import com.kavinshi.playertitle.icon.IconService;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.sync.*;
import com.kavinshi.playertitle.service.TitleEquipService;
import com.kavinshi.playertitle.service.TitleProgressService;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;

public final class RewriteBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static RewriteBootstrap instance;
    
    private IconManager iconManager;
    private IconService iconService;
    private ClusterRevisionService revisionService;
    private TitleEventFactory eventFactory;
    private TitleEquipService titleEquipService;
    private TitleProgressService titleProgressService;
    private ClusterEventBus eventBus;
    private ClusterConfig clusterConfig;

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
        
        // 初始化网络包系统
        NetworkHandler.init();
        NetworkHandler.registerPackets();
        LOGGER.info("Network handler initialized");
        
        // 初始化集群配置
        this.clusterConfig = loadClusterConfig();
        LOGGER.info("Cluster config loaded: mode={}", clusterConfig.getMode());
        
        // 初始化图标管理器
        Path configDir = Path.of("config", "playertitle", "icons");
        this.iconManager = new IconManager(configDir);
        try {
            this.iconManager.scanIcons();
            LOGGER.info("Icon manager initialized, loaded {} icons", this.iconManager.getIconCount());
        } catch (IOException e) {
            LOGGER.error("Failed to scan icons from directory: {}", configDir, e);
        }
        
        // 初始化图标服务
        this.iconService = new IconService(this.iconManager);
        LOGGER.info("Icon service initialized");
        
        // 初始化修订服务
        this.revisionService = new ClusterRevisionService();
        LOGGER.info("Revision service initialized");
        
        // 初始化事件工厂
        this.eventFactory = new TitleEventFactory(this.revisionService);
        LOGGER.info("Event factory initialized");
        
        // 根据配置初始化事件总线
        this.eventBus = createEventBus(clusterConfig);
        try {
            this.eventBus.start();
            LOGGER.info("Event bus started: {}", this.eventBus.getImplementationName());
        } catch (Exception e) {
            LOGGER.error("Failed to start event bus: {}, falling back to LOCAL", e.getMessage());
            this.eventBus = new LocalEventBus();
        }
        
        // 初始化称号服务
        this.titleEquipService = new TitleEquipService(this.eventFactory, this.eventBus);
        this.titleProgressService = new TitleProgressService(this.eventFactory, this.eventBus);
        LOGGER.info("Title services initialized");
    }
    
    private ClusterConfig loadClusterConfig() {
        // TODO: 从Forge配置文件加载
        // 目前使用默认配置（LOCAL模式）
        Path configFile = Path.of("config", "playertitle", "cluster.json");
        if (java.nio.file.Files.exists(configFile)) {
            try {
                String content = java.nio.file.Files.readString(configFile);
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
                config.getServerName(),
                config.getVelocityHost(),
                config.getVelocityPort()
            );
        };
    }
    
    public IconManager getIconManager() {
        return iconManager;
    }
    
    public IconService getIconService() {
        return iconService;
    }
    
    public ClusterRevisionService getRevisionService() {
        return revisionService;
    }
    
    public TitleEventFactory getEventFactory() {
        return eventFactory;
    }
    
    public TitleEquipService getTitleEquipService() {
        return titleEquipService;
    }
    
    public TitleProgressService getTitleProgressService() {
        return titleProgressService;
    }
    
    public ClusterEventBus getEventBus() {
        return eventBus;
    }
    
    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }
}