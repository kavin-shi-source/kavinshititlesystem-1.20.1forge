package com.kavinshi.playertitle.title;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

@SuppressWarnings("null")

public final class TitleColorUtil {
    private TitleColorUtil() {}

    public static MutableComponent buildColoredText(String text, int hexColor, ChromaType chromaType) {
        if (chromaType != null && chromaType.hasChroma()) {
            return buildChromaText(text, chromaType);
        }
        return buildStaticColorText(text, hexColor);
    }

    public static MutableComponent buildChromaText(String text, ChromaType chromaType) {
        MutableComponent result = Component.literal("");
        int totalChars = text.length();
        for (int i = 0; i < totalChars; i++) {
            int color = RainbowColorUtil.getChromaColorForChar(chromaType, i, totalChars);
            TextColor textColor = TextColor.fromRgb(color);
            MutableComponent charComponent = Component.literal(String.valueOf(text.charAt(i)));
            charComponent.withStyle(Style.EMPTY.withColor(textColor));
            result.append(charComponent);
        }
        return result;
    }

    public static MutableComponent buildStaticColorText(String text, int hexColor) {
        TextColor color = TextColor.fromRgb(hexColor & 0xFFFFFF);
        MutableComponent component = Component.literal(text);
        component.withStyle(Style.EMPTY.withColor(color));
        return component;
    }

    public static int parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) return 0xFFFFFF;
        String hexStr = colorStr.trim();
        if (hexStr.startsWith("#")) hexStr = hexStr.substring(1);
        else if (hexStr.startsWith("0x") || hexStr.startsWith("0X")) hexStr = hexStr.substring(2);
        try {
            return Integer.parseInt(hexStr, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return switch (colorStr.toLowerCase()) {
                case "black" -> 0x000000;
                case "dark_blue" -> 0x0000AA;
                case "dark_green" -> 0x00AA00;
                case "dark_aqua" -> 0x00AAAA;
                case "dark_red" -> 0xAA0000;
                case "dark_purple" -> 0xAA00AA;
                case "gold" -> 0xFFAA00;
                case "gray", "grey" -> 0xAAAAAA;
                case "dark_gray", "dark_grey" -> 0x555555;
                case "blue" -> 0x5555FF;
                case "green" -> 0x55FF55;
                case "aqua" -> 0x55FFFF;
                case "red" -> 0xFF5555;
                case "light_purple", "pink", "magenta" -> 0xFF55FF;
                case "yellow" -> 0xFFFF55;
                case "white" -> 0xFFFFFF;
                case "orange" -> 0xFF9900;
                default -> 0xFFFFFF;
            };
        }
    }
}
