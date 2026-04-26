package com.kavinshi.playertitle.title;

import com.kavinshi.playertitle.player.PlayerTitleState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TitleDisplayHelper {

    private static final Pattern ICON_PATTERN = Pattern.compile("\\[icon:([a-zA-Z0-9_]+)\\]");
    private static final Map<String, String> ICON_REGISTRY = new HashMap<>();

    static {
        // Register image icons to their unicode mappings here
        ICON_REGISTRY.put("title_1", "\uE001");
    }

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
        if (name == null || name.isEmpty()) {
            return Component.empty();
        }

        // Check if it's an icon format like [icon:title_1]
        Matcher matcher = ICON_PATTERN.matcher(name);
        if (matcher.matches()) {
            String iconKey = matcher.group(1);
            String unicodeChar = ICON_REGISTRY.get(iconKey);
            if (unicodeChar != null) {
                // Return the icon without brackets, and forced to white color to prevent tinting the image
                return Component.literal(unicodeChar).withStyle(s -> s.withColor(TextColor.fromRgb(0xFFFFFF)));
            }
        }

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
