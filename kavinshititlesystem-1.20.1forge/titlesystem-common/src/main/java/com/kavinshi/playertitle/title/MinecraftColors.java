package com.kavinshi.playertitle.title;

import java.util.Map;

public final class MinecraftColors {

    public static final Map<String, Integer> COLOR_HEX_MAP = Map.ofEntries(
        Map.entry("black", 0x000000),
        Map.entry("dark_blue", 0x0000AA),
        Map.entry("dark_green", 0x00AA00),
        Map.entry("dark_aqua", 0x00AAAA),
        Map.entry("dark_red", 0xAA0000),
        Map.entry("dark_purple", 0xAA00AA),
        Map.entry("gold", 0xFFAA00),
        Map.entry("gray", 0xAAAAAA),
        Map.entry("dark_gray", 0x555555),
        Map.entry("blue", 0x5555FF),
        Map.entry("green", 0x55FF55),
        Map.entry("aqua", 0x55FFFF),
        Map.entry("red", 0xFF5555),
        Map.entry("light_purple", 0xFF55FF),
        Map.entry("yellow", 0xFFFF55),
        Map.entry("white", 0xFFFFFF)
    );

    public static final Map<String, String> COLOR_SECTION_MAP = Map.ofEntries(
        Map.entry("black", "0"),
        Map.entry("dark_blue", "1"),
        Map.entry("dark_green", "2"),
        Map.entry("dark_aqua", "3"),
        Map.entry("dark_red", "4"),
        Map.entry("dark_purple", "5"),
        Map.entry("gold", "6"),
        Map.entry("gray", "7"),
        Map.entry("dark_gray", "8"),
        Map.entry("blue", "9"),
        Map.entry("green", "a"),
        Map.entry("aqua", "b"),
        Map.entry("red", "c"),
        Map.entry("light_purple", "d"),
        Map.entry("yellow", "e"),
        Map.entry("white", "f")
    );

    private MinecraftColors() {}

    public static int toHexColor(String colorName) {
        if (colorName == null) return 0xAAAAAA;
        Integer hex = COLOR_HEX_MAP.get(colorName.toLowerCase());
        return hex != null ? hex : 0xAAAAAA;
    }

    public static String toSectionCode(String colorName) {
        if (colorName == null) return "\u00A77";
        String code = COLOR_SECTION_MAP.get(colorName.toLowerCase());
        return code != null ? "\u00A7" + code : "\u00A77";
    }
}
