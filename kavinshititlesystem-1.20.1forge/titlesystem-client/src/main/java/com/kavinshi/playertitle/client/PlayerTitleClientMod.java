package com.kavinshi.playertitle.client;

import com.kavinshi.playertitle.ModConstants;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.PacketHandlers;
import com.kavinshi.playertitle.network.RequestSyncPacket;
import com.kavinshi.playertitle.title.TitleDefinition;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mod("playertitleclient")
/**
 * 玩家标题系统客户端主模块，负责初始化客户端组件和数据同步。
 * 注册网络数据包处理器和客户端事件监听器。
 */
public final class PlayerTitleClientMod {
    public static final String MOD_ID = "playertitleclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(PlayerTitleClientMod.class);

    public PlayerTitleClientMod() {
        NetworkHandler.init();
        NetworkHandler.registerPackets();
        registerClientPacketHandlers();
        LOGGER.info("Initializing PlayerTitle Client Module");
    }

    private void registerClientPacketHandlers() {
        PacketHandlers.registerSyncPlayerTitlesHandler(ctx -> {
            ClientTitleData.updatePlayerData(ctx.equippedTitleId, ctx.unlockedTitleIds,
                ctx.killCounts, ctx.aliveMinutes, ctx.heading);
            LOGGER.debug("Synced player titles: equipped={}, unlocked={}, heading={}",
                ctx.equippedTitleId, ctx.unlockedTitleIds.size(), ctx.heading);
        });

        PacketHandlers.registerSyncTitleRegistryHandler(ctx -> {
            ClientTitleData.updateTitleRegistry(ctx.titles);
            LOGGER.debug("Synced title registry: {} definitions", ctx.titles.size());
        });

        PacketHandlers.registerTitleUpdateClientHandler(ctx -> {
            switch (ctx.updateType) {
                case TITLE_EQUIPPED -> {
                    TitleDefinition def = ClientTitleData.getTitleById(ctx.titleId);
                    String name = def != null ? def.getName() : "";
                    int color = def != null ? def.getColor() : ModConstants.DEFAULT_COLOR;
                    String chroma = def != null ? def.getChromaType() : "NONE";
                    int color2;
                    if (def == null) {
                        color2 = 0xFFFFFF;
                    } else {
                        List<String> baseColors = def.getBaseColors();
                        if (baseColors != null && baseColors.size() >= 2) {
                            try {
                                color2 = Integer.parseInt(baseColors.get(1).replace("#", ""), 16);
                            } catch (NumberFormatException e) {
                                color2 = def.getColor();
                            }
                        } else {
                            color2 = def.getColor();
                        }
                    }
                    ClientTitleData.updatePlayerEquippedTitle(ctx.playerId, ctx.titleId, name, color, chroma, color2);
                }
                case TITLE_UNLOCKED -> ClientTitleData.addUnlockedTitle(ctx.titleId);
                case TITLE_REVOKED -> ClientTitleData.removeUnlockedTitle(ctx.titleId);
                case KILL_COUNT_UPDATED -> ClientTitleData.updateKillCount(ctx.entityId, ctx.count);
                case ALIVE_TIME_UPDATED -> ClientTitleData.updateAliveMinutes(ctx.count);
            }
        });
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            var player = event.getPlayer();
            if (player != null) {
                NetworkHandler.getChannel().sendToServer(new RequestSyncPacket(player.getUUID(), true));
                LOGGER.debug("Sent sync request for player: {}", player.getUUID());
            }
        }
    }

}
