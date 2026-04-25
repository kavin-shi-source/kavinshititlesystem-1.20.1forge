package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.TitleDisplayHelper;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)

@SuppressWarnings("null")
public final class ChatHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent titleComponent = TitleDisplayHelper.buildTitleComponent(state, registry);
            if (titleComponent == null) return;

            Component rawDisplayName = player.getDisplayName();
            if (rawDisplayName == null) rawDisplayName = Component.literal(player.getScoreboardName());
            Component displayNameForHover = rawDisplayName;
            HoverEvent.EntityTooltipInfo entityInfo = new HoverEvent.EntityTooltipInfo(
                EntityType.PLAYER, player.getUUID(), displayNameForHover);
            MutableComponent playerName = rawDisplayName.copy()
                .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY, entityInfo)));

            MutableComponent newMessage = Component.literal("")
                    .append(titleComponent)
                    .append(Component.literal(" "))
                    .append(playerName)
                    .append(Component.literal(": "))
                    .append(event.getMessage());

            event.setCanceled(true);
            var server = player.server;
            for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                target.sendSystemMessage(newMessage);
            }
        });
    }
}
