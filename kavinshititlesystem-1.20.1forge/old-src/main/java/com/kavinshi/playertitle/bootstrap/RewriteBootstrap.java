package com.kavinshi.playertitle.bootstrap;

import com.kavinshi.playertitle.icon.IconManager;
import com.kavinshi.playertitle.icon.IconService;
import com.kavinshi.playertitle.sync.ClusterEventBus;
import com.kavinshi.playertitle.sync.ClusterRevisionService;
import com.kavinshi.playertitle.sync.LocalEventBus;
import com.kavinshi.playertitle.sync.TitleEventFactory;
import com.kavinshi.playertitle.service.TitleEquipService;
import com.kavinshi.playertitle.service.TitleProgressService;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;

/**
 * Central composition root for rewrite services.
 * The first task only establishes a stable place for future wiring.
 */
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
        
        // Initialize icon manager with default config directory
        Path configDir = Path.of("config", "playertitle", "icons");
        this.iconManager = new IconManager(configDir);
        try {
            this.iconManager.scanIcons();
            LOGGER.info("Icon manager initialized, loaded {} icons", this.iconManager.getIconCount());
        } catch (IOException e) {
            LOGGER.error("Failed to scan icons from directory: {}", configDir, e);
            // Continue without icons
        }
        
        // Initialize icon service
        this.iconService = new IconService(this.iconManager);
        LOGGER.info("Icon service initialized");
        
        // Initialize revision service
        this.revisionService = new ClusterRevisionService();
        LOGGER.info("Revision service initialized");
        
        // Initialize event factory
        this.eventFactory = new TitleEventFactory(this.revisionService);
        LOGGER.info("Event factory initialized");
        
        // Initialize event bus - start with local event bus
        // TODO: Make this configurable to switch to RedisEventBus
        this.eventBus = new LocalEventBus();
        try {
            this.eventBus.start();
            LOGGER.info("Event bus started: {}", this.eventBus.getImplementationName());
        } catch (Exception e) {
            LOGGER.error("Failed to start event bus", e);
            // Fall back to local event bus that doesn't need startup
            this.eventBus = new LocalEventBus();
        }
        
        // Initialize title services
        this.titleEquipService = new TitleEquipService(this.eventFactory, this.eventBus);
        this.titleProgressService = new TitleProgressService(this.eventFactory, this.eventBus);
        LOGGER.info("Title services initialized");
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
}
