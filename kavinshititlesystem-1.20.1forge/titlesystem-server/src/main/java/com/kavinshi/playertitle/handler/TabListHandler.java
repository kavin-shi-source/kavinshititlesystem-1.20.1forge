package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TabListHandler {
    private static int tickCounter = 0;
    private static int currentPlayerIndex = 0;
    private static Boolean hasTabMod = null;
    
    private static boolean hasTabMod() {
        if (hasTabMod == null) {
            hasTabMod = ModList.get().isLoaded("tab") ||
                        ModList.get().isLoaded("bettertab") ||
                        ModList.get().isLoaded("tabby") ||
                        ModList.get().isLoaded("vanillatweaks") ||
                        ModList.get().isLoaded("vt") ||
                        ModList.get().isLoaded("essential") ||
                        ModList.get().isLoaded("velocity") ||
                        ModList.get().isLoaded("velocitytab") ||
                        ModList.get().isLoaded("vtab") ||
                        ModList.get().isLoaded("vtabmod") ||
                        ModList.get().isLoaded("fabrictab") ||
                        ModList.get().isLoaded("playerlist") ||
                        ModList.get().isLoaded("playerlistmod") ||
                        ModList.get().isLoaded("tablist");
            if (hasTabMod) {
                System.out.println("[PlayerTitle] Tab plugin detected on server, skipping custom tab list modifications");
            }
        }
        return hasTabMod;
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TitleConfig.SERVER.customTabList.get()) return;
        if (hasTabMod()) return;

        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent displayName = buildTabDisplayName(state, player);
            if (displayName != null) {
                event.setDisplayName(displayName);
            }
        });
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!TitleConfig.SERVER.customTabList.get()) return;
        if (hasTabMod()) return;

        int updateInterval = TitleConfig.SERVER.tabListUpdateInterval.get();
        boolean framePacing = TitleConfig.SERVER.enableFramePacing.get();
        
        tickCounter++;
        var players = event.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (framePacing) {
            // 分帧处理模式
            // 检查是否达到更新间隔
            if (tickCounter % updateInterval == 0) {
                // 重置玩家索引，开始新一轮的分帧处理
                currentPlayerIndex = 0;
            }

            // 如果当前没有玩家需要处理，直接返回
            if (currentPlayerIndex >= players.size()) return;

            // 分帧处理：每tick只处理一个玩家
            ServerPlayer player = players.get(currentPlayerIndex);
            TitleCapability.get(player).ifPresent(state -> {
                MutableComponent displayName = buildTabDisplayName(state, player);
                if (displayName != null) {
                    player.refreshTabListName();
                }
            });

            currentPlayerIndex++;
            // 如果已经处理完所有玩家，等待下一个更新间隔
            if (currentPlayerIndex >= players.size()) {
                currentPlayerIndex = players.size(); // 设置为超出范围，避免继续处理
            }
        } else {
            // 传统模式：达到间隔时一次性处理所有玩家
            if (tickCounter % updateInterval != 0) return;
            
            for (ServerPlayer player : players) {
                TitleCapability.get(player).ifPresent(state -> {
                    MutableComponent displayName = buildTabDisplayName(state, player);
                    if (displayName != null) {
                        player.refreshTabListName();
                    }
                });
            }
        }
    }

    private static MutableComponent buildTabDisplayName(PlayerTitleState state, ServerPlayer player) {
        CustomTitleData ct = state.getCustomTitle();
        if (ct.isUsingCustomTitle() && ct.hasPermission()) {
            String display = "[" + ct.getText() + "] ";
            MutableComponent titleComp = Component.literal(display)
                .withStyle(s -> s.withColor(TextColor.fromRgb(ct.getColor1() & 0xFFFFFF)));
            return titleComp.append(player.getDisplayName());
        }

        int titleId = state.getEquippedTitleId();
        if (titleId < 0) return null;

        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
        TitleDefinition title = registry.getTitle(titleId);
        if (title == null) return null;

        String display = "[" + title.getName() + "] ";
        MutableComponent titleComp = Component.literal(display);
        int color = title.getColor();
        if (color != 0xFFFFFF) {
            titleComp.withStyle(s -> s.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return titleComp.append(player.getDisplayName());
    }
}
