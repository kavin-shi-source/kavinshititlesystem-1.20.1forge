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

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TabListHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TitleConfig.SERVER.customTabList.get()) return;

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

        tickCounter++;
        if (tickCounter % 100 != 0) return;

        var server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TitleCapability.get(player).ifPresent(state -> {
                MutableComponent displayName = buildTabDisplayName(state, player);
                if (displayName != null) {
                    player.refreshTabListName();
                }
            });
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
