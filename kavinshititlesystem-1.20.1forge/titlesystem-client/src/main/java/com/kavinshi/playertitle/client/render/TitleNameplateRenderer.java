package com.kavinshi.playertitle.client.render;

import com.kavinshi.playertitle.client.ClientTitleData;
import com.kavinshi.playertitle.title.ChromaType;
import com.kavinshi.playertitle.title.RainbowColorUtil;
import com.kavinshi.playertitle.title.TitleDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
    bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE,
    value = net.minecraftforge.api.distmarker.Dist.CLIENT
)
@SuppressWarnings("null")
public final class TitleNameplateRenderer {

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderNameplate(RenderNameTagEvent event) {
        RainbowColorUtil.updateCachedTime();
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getResult() == Event.Result.DENY) return;

        if (player.isInvisible()) return;
        var cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) return;
        double distSq = player.distanceToSqr(cameraEntity);
        if (distSq > 4096.0) return;

        boolean isSelf = player == Minecraft.getInstance().player;

        ClientTitleData.EquippedTitleInfo info = ClientTitleData.getEquippedTitleForPlayer(player.getUUID());
        if (info == null || info.titleId < 0) return;

        Component cachedTitle = ClientTitleData.getCachedTitleComponent(player.getUUID());
        if (cachedTitle == null) return;

        MutableComponent newContent = Component.empty().append(cachedTitle).append(Component.literal(" ")).append(event.getContent());
        event.setContent(newContent);
    }

}
