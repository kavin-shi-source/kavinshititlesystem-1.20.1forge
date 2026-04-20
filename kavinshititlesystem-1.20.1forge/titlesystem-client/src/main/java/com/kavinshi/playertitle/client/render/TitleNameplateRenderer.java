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
public final class TitleNameplateRenderer {

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderNameplate(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getResult() == Event.Result.DENY) return;

        if (player.isInvisible() || player == Minecraft.getInstance().player) return;
        double distSq = player.distanceToSqr(Minecraft.getInstance().getCameraEntity());
        if (distSq > 4096.0) return;

        ClientTitleData.EquippedTitleInfo info = ClientTitleData.getEquippedTitleForPlayer(player.getUUID());
        if (info == null || info.titleId < 0) return;

        String titleName = info.titleName != null && !info.titleName.isEmpty()
            ? info.titleName : findTitleName(info.titleId);
        if (titleName == null) return;

        ChromaType chroma = ChromaType.fromString(info.chromaType);

        MutableComponent bracket = Component.literal("[ ");
        int frameColor = chroma.getFrameColor();
        bracket.withStyle(style -> style.withColor(TextColor.fromRgb(frameColor)));

        MutableComponent titleText = chroma.hasChroma()
            ? buildChromaComponent(titleName, chroma)
            : Component.literal(titleName).withStyle(style -> style.withColor(TextColor.fromRgb(info.titleColor & 0xFFFFFF)));

        MutableComponent closeBracket = Component.literal(" ]");
        closeBracket.withStyle(style -> style.withColor(TextColor.fromRgb(frameColor)));

        event.setContent(bracket.append(titleText).append(closeBracket).append(event.getContent()));
    }

    private static MutableComponent buildChromaComponent(String text, ChromaType chroma) {
        MutableComponent result = Component.literal("");
        for (int i = 0; i < text.length(); i++) {
            int color = RainbowColorUtil.getChromaColorForChar(chroma, i, text.length());
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(s -> s.withColor(TextColor.fromRgb(color))));
        }
        return result;
    }

    private static String findTitleName(int titleId) {
        for (TitleDefinition def : ClientTitleData.getTitleRegistry())
            if (def.getId() == titleId) return def.getName();
        return null;
    }
}
