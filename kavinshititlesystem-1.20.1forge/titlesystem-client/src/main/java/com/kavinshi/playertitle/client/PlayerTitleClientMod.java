package com.kavinshi.playertitle.client;

import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.PacketHandlers;
import com.kavinshi.playertitle.network.RequestSyncPacket;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;

@Mod("playertitleclient")
public final class PlayerTitleClientMod {
    public static final String MOD_ID = "playertitleclient";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlayerTitleClientMod() {
        NetworkHandler.init();
        NetworkHandler.registerPackets();
        registerClientPacketHandlers();
        LOGGER.info("Initializing PlayerTitle Client Module");
    }

    private void registerClientPacketHandlers() {
        PacketHandlers.registerSyncPlayerTitlesHandler(ctx -> {
            ClientTitleData.updatePlayerData(ctx.equippedTitleId, ctx.unlockedTitleIds, ctx.killCounts, ctx.aliveMinutes);
            LOGGER.debug("Synced player titles: equipped={}, unlocked={}", ctx.equippedTitleId, ctx.unlockedTitleIds.size());
        });

        PacketHandlers.registerSyncTitleRegistryHandler(ctx -> {
            ClientTitleData.updateTitleRegistry(ctx.titles);
            LOGGER.debug("Synced title registry: {} definitions", ctx.titles.size());
        });

        PacketHandlers.registerTitleUpdateClientHandler(ctx -> {
            switch (ctx.updateType) {
                case TITLE_EQUIPPED -> {
                    String name = findTitleName(ctx.titleId);
                    int color = findTitleColor(ctx.titleId);
                    String chroma = findTitleChroma(ctx.titleId);
                    ClientTitleData.updatePlayerEquippedTitle(ctx.playerId, ctx.titleId, name, color, chroma);
                }
                case TITLE_UNLOCKED -> ClientTitleData.addUnlockedTitle(ctx.titleId);
                case TITLE_REVOKED -> ClientTitleData.removeUnlockedTitle(ctx.titleId);
                case KILL_COUNT_UPDATED -> ClientTitleData.updateKillCount(ctx.entityId, ctx.count);
                case ALIVE_TIME_UPDATED -> ClientTitleData.updateAliveMinutes(ctx.count);
            }
        });
    }

    public static class ClientEvents {
        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            if (Minecraft.getInstance().player != null) {
                UUID playerId = Minecraft.getInstance().player.getUUID();
                NetworkHandler.getChannel().sendToServer(new RequestSyncPacket(playerId, true));
                LOGGER.debug("Sent sync request for player: {}", playerId);
            }
        }
    }

    private static String findTitleName(int titleId) {
        for (TitleDefinition def : ClientTitleData.getTitleRegistry())
            if (def.getId() == titleId) return def.getName();
        return "";
    }

    private static int findTitleColor(int titleId) {
        for (TitleDefinition def : ClientTitleData.getTitleRegistry())
            if (def.getId() == titleId) return def.getColor();
        return 0xFFFFFF;
    }

    private static String findTitleChroma(int titleId) {
        for (TitleDefinition def : ClientTitleData.getTitleRegistry())
            if (def.getId() == titleId) return def.getChromaType();
        return "NONE";
    }
}
