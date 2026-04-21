package com.kavinshi.playertitle.client;

import com.kavinshi.playertitle.ModConstants;
import java.nio.file.Path;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.resource.PathPackResources;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;

/**
 * 客户端模组初始化器，负责注册图标字体资源包。
 * 使用Forge的事件总线在客户端启动时添加资源包。
 */
@Mod.EventBusSubscriber(modid = "playertitleclient", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static ClientIconManager clientIconManager;
    private static boolean initialized = false;
    
    /**
     * 初始化客户端图标管理器并生成资源包。
     * 在客户端设置事件中调用。
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!initialized) {
            initialize();
            initialized = true;
        }
    }
    
    private static void initialize() {
        LOGGER.info("Initializing PlayerTitle client...");
        
        Path configDir = FMLLoader.getGamePath().resolve("config").resolve("playertitle").resolve("icons");
        clientIconManager = new ClientIconManager(configDir);
        
        try {
            clientIconManager.scanIcons();
            LOGGER.info("Client icon manager loaded {} icons", clientIconManager.getIconCount());
            
            // 生成资源包
            Path resourcePackDir = clientIconManager.getDefaultResourcePackDir();
            clientIconManager.generateFontResourcePack(resourcePackDir);
            LOGGER.info("Font resource pack generated at: {}", resourcePackDir);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize client icon manager", e);
        }
    }
    
    /**
     * 获取客户端图标管理器实例。
     *
     * @return 客户端图标管理器
     */
    public static ClientIconManager getClientIconManager() {
        if (!initialized) {
            initialize();
            initialized = true;
        }
        return clientIconManager;
    }
    
    /**
     * 在添加包查找器时注册资源包。
     * 这将使资源包在游戏资源包列表中可用。
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            LOGGER.info("Adding PlayerTitle font resource pack to pack finders");
            
            Path resourcePackDir = getClientIconManager().getDefaultResourcePackDir();
            Path packMeta = resourcePackDir.resolve("pack.mcmeta");
            
            if (!java.nio.file.Files.exists(packMeta)) {
                LOGGER.warn("Resource pack meta file not found: {}", packMeta);
                return;
            }
            
            try {
                // 创建PathPackResources实例
                String packId = "playertitleclient_fonts";
                PathPackResources packResources = new PathPackResources(
                    packId,
                    false,
                    resourcePackDir
                );
                
                // 创建Pack实例
                Pack pack = Pack.readMetaAndCreate(
                    packId,
                    Component.literal("PlayerTitle Fonts"),
                    true,
                    (path) -> packResources,
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.DEFAULT
                );
                
                if (pack != null) {
                    event.addRepositorySource((consumer) -> consumer.accept(pack));
                    LOGGER.info("PlayerTitle font resource pack registered: {}", packId);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to register PlayerTitle font resource pack", e);
            }
        }
    }
}