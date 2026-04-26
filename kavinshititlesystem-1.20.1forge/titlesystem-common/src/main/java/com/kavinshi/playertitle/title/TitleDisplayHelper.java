package com.kavinshi.playertitle.title;

import com.kavinshi.playertitle.player.PlayerTitleState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public final class TitleDisplayHelper {

    private TitleDisplayHelper() {}

    public static MutableComponent buildTitleComponent(PlayerTitleState state, TitleRegistry registry) {
        int titleId = state.getEquippedTitleId();
        if (titleId < 0) return Component.empty();

        TitleDefinition title = registry.getTitle(titleId);
        if (title == null) return Component.empty();

        return createTitleComponent(title);
    }

    public static MutableComponent createTitleComponent(TitleDefinition title) {
        String name = title.getName();
        int color = title.getColor();
        return createTitleComponent(name, color, ChromaType.NONE, 0xFFFFFF);
    }

    public static MutableComponent createTitleComponent(String name, int color, ChromaType chromaType, int color2) {
        int frameColor = chromaType.getFrameColor();
        MutableComponent bracket = Component.literal("[ ").withStyle(s -> s.withColor(TextColor.fromRgb(frameColor)));
        MutableComponent closeBracket = Component.literal(" ]").withStyle(s -> s.withColor(TextColor.fromRgb(frameColor)));

        MutableComponent titleText;
        if (chromaType == ChromaType.CUSTOM_GRADIENT) {
            titleText = buildGradientComponent(name, color, color2);
        } else if (chromaType.hasChroma()) {
            titleText = buildChromaComponent(name, chromaType);
        } else {
            titleText = Component.literal(name).withStyle(s -> s.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }

        return bracket.append(titleText).append(closeBracket);
    }

    private static MutableComponent buildChromaComponent(String text, ChromaType chroma) {
        MutableComponent result = Component.literal("");
        for (int i = 0; i < text.length(); i++) {
            int charColor = RainbowColorUtil.getChromaColorForChar(chroma, i, text.length());
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(s -> s.withColor(TextColor.fromRgb(charColor))));
        }
        return result;
    }

    private static MutableComponent buildGradientComponent(String text, int color1, int color2) {
        MutableComponent result = Component.literal("");
        for (int i = 0; i < text.length(); i++) {
            int charColor = RainbowColorUtil.getGradientColorForChar(color1, color2, i, text.length());
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(s -> s.withColor(TextColor.fromRgb(charColor))));
        }
        return result;
    }

    public static MutableComponent createTabPrefix(PlayerTitleState state, TitleRegistry registry) {
        int titleId = state.getEquippedTitleId();
        if (titleId < 0) return Component.empty();

        TitleDefinition title = registry.getTitle(titleId);
        if (title == null) return Component.empty();

        String display = "[" + title.getName() + "] ";
        MutableComponent titleComp = Component.literal(display);
        int color = title.getColor();
        if (color != 0xFFFFFF) {
            titleComp.withStyle(s -> s.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return titleComp;
    }
}
