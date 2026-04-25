package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.TitleDisplayHelper;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("null")
public final class JoinLeaveHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TitleConfig.SERVER.customJoinLeave.get()) return;

        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent titleComp = TitleDisplayHelper.buildTitleComponent(state, registry);
            if (titleComp == null) return;

            MutableComponent joinMessage = Component.literal("")
                    .append(titleComp)
                    .append(Component.literal(" "))
                    .append(player.getDisplayName())
                    .append(Component.literal(" joined the game"));

            var server = player.server;
            for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                target.sendSystemMessage(joinMessage);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TitleConfig.SERVER.customJoinLeave.get()) return;

        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent titleComp = TitleDisplayHelper.buildTitleComponent(state, registry);
            if (titleComp == null) return;

            MutableComponent leaveMessage = Component.literal("")
                    .append(titleComp)
                    .append(Component.literal(" "))
                    .append(Component.literal(player.getName().getString()))
                    .append(Component.literal(" left the game"));

            var server = player.server;
            if (server != null) {
                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    target.sendSystemMessage(leaveMessage);
                }
            }
        });
    }
}
