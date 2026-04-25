package com.kavinshi.playertitle.client.render;

import com.kavinshi.playertitle.client.ClientTitleData;
import com.kavinshi.playertitle.title.ChromaType;
import com.kavinshi.playertitle.title.CustomTitleData;
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
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getResult() == Event.Result.DENY) return;

        if (player.isInvisible() || player == Minecraft.getInstance().player) return;
        double distSq = player.distanceToSqr(Minecraft.getInstance().getCameraEntity());
        if (distSq > 4096.0) return;

        boolean isSelf = player.getUUID().equals(Minecraft.getInstance().player.getUUID());
        String titleName;
        ChromaType chroma;
        int titleColor;

        if (isSelf && ClientTitleData.isUsingCustomTitle()) {
            CustomTitleData ct = ClientTitleData.getCustomTitle();
            titleName = ct.getText();
            chroma = ct.getEffectiveChromaType();
            titleColor = ct.getColor1();
        } else {
            ClientTitleData.EquippedTitleInfo info = ClientTitleData.getEquippedTitleForPlayer(player.getUUID());
            if (info == null || info.titleId < 0) return;
            titleName = info.titleName != null && !info.titleName.isEmpty()
                ? info.titleName : findTitleName(info.titleId);
            if (titleName == null) return;
            chroma = ChromaType.fromString(info.chromaType);
            titleColor = info.titleColor;
        }

        if (titleName.isEmpty()) return;

        int frameColor = chroma.getFrameColor();
        MutableComponent bracket = Component.literal("[ ").withStyle(s -> s.withColor(TextColor.fromRgb(frameColor)));
        MutableComponent closeBracket = Component.literal(" ]").withStyle(s -> s.withColor(TextColor.fromRgb(frameColor)));

        MutableComponent titleText;
        if (chroma == ChromaType.CUSTOM_GRADIENT && isSelf) {
            CustomTitleData ct = ClientTitleData.getCustomTitle();
            titleText = buildGradientComponent(titleName, ct.getColor1(), ct.getColor2());
        } else if (chroma.hasChroma()) {
            titleText = buildChromaComponent(titleName, chroma);
        } else {
            titleText = Component.literal(titleName).withStyle(s -> s.withColor(TextColor.fromRgb(titleColor & 0xFFFFFF)));
        }

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

    private static MutableComponent buildGradientComponent(String text, int color1, int color2) {
        MutableComponent result = Component.literal("");
        for (int i = 0; i < text.length(); i++) {
            int color = RainbowColorUtil.getGradientColorForChar(color1, color2, i, text.length());
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
