package com.kavinshi.playertitle;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.network.PacketHandlers;
import com.kavinshi.playertitle.network.TitleUpdatePacket;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.handler.BuffHandler;
import com.kavinshi.playertitle.handler.TitleSyncHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("playertitleserver")
public final class KavinshiPlayerTitleMod {
    public static final String MOD_ID = "playertitleserver";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KavinshiPlayerTitleMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TitleConfig.SERVER_SPEC);

        registerServerPacketHandlers();

        RewriteBootstrap.initialize();
        LOGGER.info("Initializing PlayerTitle Server Module");
    }

    private void registerServerPacketHandlers() {
        PacketHandlers.registerTitleUpdateServerHandler(ctx -> {
            if (ctx.updateType != TitleUpdatePacket.UpdateType.TITLE_EQUIPPED) return;
            if (ctx.titleId == -1) {
                BuffHandler.removeBuffs(ctx.sender, ctx.sender.getCapability(TitleCapability.CAPABILITY)
                    .map(s -> s.getEquippedTitleId()).orElse(-1));
                TitleCapability.get(ctx.sender).ifPresent(state -> {
                    state.setEquippedTitleId(-1);
                    TitleSyncHandler.syncPlayerData(ctx.sender);
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
}
