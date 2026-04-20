package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChatHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent titleComponent = null;

            CustomTitleData ct = state.getCustomTitle();
            if (ct.isUsingCustomTitle() && ct.hasPermission()) {
                titleComponent = createCustomTitleComponent(ct);
            } else {
                int titleId = state.getEquippedTitleId();
                if (titleId < 0) return;
                TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
                TitleDefinition title = registry.getTitle(titleId);
                if (title == null) return;
                titleComponent = createTitleComponent(title);
            }

            HoverEvent.EntityTooltipInfo entityInfo = new HoverEvent.EntityTooltipInfo(
                EntityType.PLAYER, player.getUUID(), player.getDisplayName());
            MutableComponent playerName = player.getDisplayName().copy()
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

    private static MutableComponent createTitleComponent(TitleDefinition title) {
        String display = "[" + title.getName() + "]";
        MutableComponent component = Component.literal(display);
        int color = title.getColor();
        if (color != 0xFFFFFF) {
            component.withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return component;
    }

    private static MutableComponent createCustomTitleComponent(CustomTitleData ct) {
        String display = "[" + ct.getText() + "]";
        MutableComponent component = Component.literal(display);
        int color = ct.getColor1();
        if (color != 0xFFFFFF) {
            component.withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return component;
    }
}
