package com.kavinshi.playertitle.client;

import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.PacketHandlers;
import com.kavinshi.playertitle.network.RequestSyncPacket;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("playertitleclient")
/**
 * 玩家标题系统客户端主模块，负责初始化客户端组件和数据同步。
 * 注册网络数据包处理器和客户端事件监听器。
 */
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
            ClientTitleData.updatePlayerData(ctx.equippedTitleId, ctx.unlockedTitleIds,
                ctx.killCounts, ctx.aliveMinutes, ctx.customTitle);
            LOGGER.debug("Synced player titles: equipped={}, unlocked={}, custom={}",
                ctx.equippedTitleId, ctx.unlockedTitleIds.size(),
                ctx.customTitle != null ? ctx.customTitle.getPermissionName() : "null");
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

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                NetworkHandler.getChannel().sendToServer(new RequestSyncPacket(player.getUUID(), true));
                LOGGER.debug("Sent sync request for player: {}", player.getUUID());
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
