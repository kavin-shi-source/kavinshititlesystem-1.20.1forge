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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JoinLeaveHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TitleConfig.SERVER.customJoinLeave.get()) return;

        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent titleComp = buildTitleComponent(state);
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

        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent titleComp = buildTitleComponent(state);
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

    private static MutableComponent buildTitleComponent(PlayerTitleState state) {
        CustomTitleData ct = state.getCustomTitle();
        if (ct.isUsingCustomTitle() && ct.hasPermission()) {
            return createCustomTitleComponent(ct);
        }

        int titleId = state.getEquippedTitleId();
        if (titleId < 0) return null;

        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
        TitleDefinition title = registry.getTitle(titleId);
        if (title == null) return null;

        return createTitleComponent(title);
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
