package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.player.TitleCapability;
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

        TitleCapability.get(player).ifPresent(state -> {
            int titleId = state.getEquippedTitleId();
            if (titleId < 0) return;

            TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
            TitleDefinition title = registry.getTitle(titleId);
            if (title == null) return;

            MutableComponent titleComp = createTitleComponent(title);
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

    private static MutableComponent createTitleComponent(TitleDefinition title) {
        String display = "[" + title.getName() + "]";
        MutableComponent component = Component.literal(display);
        int color = title.getColor();
        if (color != 0xFFFFFF) {
            component.withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return component;
    }
}
