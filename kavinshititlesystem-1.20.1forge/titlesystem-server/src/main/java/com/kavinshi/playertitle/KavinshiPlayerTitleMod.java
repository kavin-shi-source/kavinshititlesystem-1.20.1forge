package com.kavinshi.playertitle;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.network.ClusterSyncPacket;
import com.kavinshi.playertitle.network.PacketHandlers;
import com.kavinshi.playertitle.network.TitleUpdatePacket;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.handler.BuffHandler;
import com.kavinshi.playertitle.handler.TitleSyncHandler;
import com.kavinshi.playertitle.sync.ClusterMode;
import com.kavinshi.playertitle.sync.VelocityEventBus;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("playertitleserver")
public final class KavinshiPlayerTitleMod {
    public static final String MOD_ID = "playertitleserver";
    public static final Logger LOGGER = LoggerFactory.getLogger(KavinshiPlayerTitleMod.class);

    @SuppressWarnings("removal")
    public KavinshiPlayerTitleMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TitleConfig.SERVER_SPEC);

        registerServerPacketHandlers();

        RewriteBootstrap.initialize();

        registerClusterSyncHandler();

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);

        LOGGER.info("Initializing PlayerTitle Server Module");
    }

    private void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        RewriteBootstrap.getInstance().onServerStarting(server);
        LOGGER.info("PlayerTitleDataBridge initialized on server start");
    }

    private void onServerStopped(ServerStoppedEvent event) {
        MinecraftServer server = event.getServer();
        RewriteBootstrap.getInstance().onServerStopping(server);
        LOGGER.info("PlayerTitle Server Module stopped");
    }

    private void registerServerPacketHandlers() {
        PacketHandlers.registerTitleUpdateServerHandler(ctx -> {
            if (ctx.updateType != TitleUpdatePacket.UpdateType.TITLE_EQUIPPED) return;
            if (ctx.titleId == -1) {
                TitleCapability.get(ctx.sender).ifPresent(state -> {
                    int oldId = state.getEquippedTitleId();
                    if (oldId >= 0) {
                        BuffHandler.removeBuffs(ctx.sender, oldId);
                    }
                    state.setEquippedTitleId(-1);
                    com.kavinshi.playertitle.database.DatabaseAsyncWriter.queueWrite(
                            RewriteBootstrap.getInstance().getTitleRepository(), state);
                    TitleSyncHandler.syncPlayerData(ctx.sender);
                    TitleSyncHandler.broadcastEquippedTitle(ctx.sender);
                });
            } else {
                TitleCapability.get(ctx.sender).ifPresent(state -> {
                    int oldId = state.getEquippedTitleId();
                    com.kavinshi.playertitle.service.EquipResult result =
                        RewriteBootstrap.getInstance().getTitleEquipService()
                            .equip(state, RewriteBootstrap.getInstance().getTitleRegistry(), ctx.titleId);
                    if (result.success()) {
                        if (oldId >= 0) BuffHandler.removeBuffs(ctx.sender, oldId);
                        BuffHandler.applyBuffs(ctx.sender, ctx.titleId);
                        com.kavinshi.playertitle.database.DatabaseAsyncWriter.queueWrite(
                                RewriteBootstrap.getInstance().getTitleRepository(), state);
                        TitleSyncHandler.syncPlayerData(ctx.sender);
                        TitleSyncHandler.broadcastEquippedTitle(ctx.sender);
                    }
                });
            }
        });

        PacketHandlers.registerRequestSyncHandler(ctx -> {
            TitleSyncHandler.syncPlayerData(ctx.sender);
            TitleSyncHandler.syncAllEquippedTitlesToPlayer(ctx.sender);
            TitleSyncHandler.syncTitleRegistryToPlayer(ctx.sender);
        });
    }

    private void registerClusterSyncHandler() {
        var config = RewriteBootstrap.getInstance().getClusterConfig();
        if (config.getMode() != ClusterMode.VELOCITY) {
            return;
        }

        var eventBus = RewriteBootstrap.getInstance().getEventBus();
        if (!(eventBus instanceof VelocityEventBus velocityBus)) {
            return;
        }

        PacketHandlers.registerClusterSyncHandler(ctx -> {
            ClusterSyncPacket packet = new ClusterSyncPacket(
                ctx.sourceServer, ctx.eventType, ctx.playerId,
                ctx.revision, ctx.timestampMs, ctx.payload
            );
            velocityBus.onClusterSyncPacket(packet);
        });

        LOGGER.info("Registered cluster sync handler for Velocity mode");
    }
}
