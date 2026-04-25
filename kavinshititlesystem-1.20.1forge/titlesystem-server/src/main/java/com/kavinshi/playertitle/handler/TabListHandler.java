package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.TitleDisplayHelper;
import com.kavinshi.playertitle.title.TitleRegistry;
import com.kavinshi.playertitle.util.TabModDetector;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("null")
public final class TabListHandler {
    private static int tickCounter = 0;
    private static int currentPlayerIndex = 0;

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TitleConfig.SERVER.customTabList.get()) return;
        if (TabModDetector.hasTabMod()) return;

        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent prefix = TitleDisplayHelper.createTabPrefix(state, registry);
            if (prefix != null) {
                event.setDisplayName(prefix.append(player.getDisplayName()));
            }
        });
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!TitleConfig.SERVER.customTabList.get()) return;
        if (TabModDetector.hasTabMod()) return;

        int updateInterval = TitleConfig.SERVER.tabListUpdateInterval.get();
        boolean framePacing = TitleConfig.SERVER.enableFramePacing.get();

        tickCounter++;
        var players = event.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();

        if (framePacing) {
            if (tickCounter % updateInterval == 0) {
                currentPlayerIndex = 0;
            }

            if (currentPlayerIndex >= players.size()) return;

            ServerPlayer player = players.get(currentPlayerIndex);
            TitleCapability.get(player).ifPresent(state -> {
                MutableComponent prefix = TitleDisplayHelper.createTabPrefix(state, registry);
                if (prefix != null) {
                    player.refreshTabListName();
                }
            });

            currentPlayerIndex++;
            if (currentPlayerIndex >= players.size()) {
                currentPlayerIndex = players.size();
            }
        } else {
            if (tickCounter % updateInterval != 0) return;

            for (ServerPlayer player : players) {
                TitleCapability.get(player).ifPresent(state -> {
                    MutableComponent prefix = TitleDisplayHelper.createTabPrefix(state, registry);
                    if (prefix != null) {
                        player.refreshTabListName();
                    }
                });
            }
        }
    }
}
