package com.kavinshi.playertitle.title;

/**
 * 色彩类型枚举，定义称号名称的染色效果和稀有度等级。
 * 每种色彩类型对应不同的视觉样式和稀有度。
 */
public enum ChromaType {
    NONE,
    WHITE_GRAY,
    GREEN_WHITE,
    CYAN_WHITE,
    BLUE_WHITE,
    RAINBOW,
    BLACK_WHITE,
    RED_BLACK,
    DARK_RED_PURPLE,
    GOLD_BLACK,
    CUSTOM_GRADIENT;

    public boolean hasChroma() {
        return this != NONE;
    }

    public int getRarityTier() {
        return switch (this) {
            case NONE -> 0;
            case WHITE_GRAY -> 1;
            case GREEN_WHITE -> 2;
            case CYAN_WHITE -> 3;
            case BLUE_WHITE -> 4;
            case RAINBOW -> 5;
            case BLACK_WHITE -> 6;
            case RED_BLACK -> 7;
            case DARK_RED_PURPLE -> 8;
            case GOLD_BLACK -> 9;
            case CUSTOM_GRADIENT -> 4;
        };
    }

    public String getRarityName() {
        return switch (this) {
            case NONE -> "None";
            case WHITE_GRAY -> "Common";
            case GREEN_WHITE -> "Uncommon";
            case CYAN_WHITE -> "Rare";
            case BLUE_WHITE -> "Epic";
            case RAINBOW -> "Legendary";
            case BLACK_WHITE -> "Mythical";
            case RED_BLACK -> "Supreme";
            case DARK_RED_PURPLE -> "Apex";
            case GOLD_BLACK -> "Ultimate";
            case CUSTOM_GRADIENT -> "Custom";
        };
    }

    public int getRarityDisplayColor() {
        return switch (this) {
            case NONE -> 0xFF6B6B6B;
            case WHITE_GRAY -> 0xFFBBBBBB;
            case GREEN_WHITE -> 0xFF55FF55;
            case CYAN_WHITE -> 0xFF55FFFF;
            case BLUE_WHITE -> 0xFF5555FF;
            case RAINBOW -> 0xFFFFAA00;
            case BLACK_WHITE -> 0xFFAAAAAA;
            case RED_BLACK -> 0xFFFF5555;
            case DARK_RED_PURPLE -> 0xFFCC6699;
            case GOLD_BLACK -> 0xFFFFCC00;
            case CUSTOM_GRADIENT -> 0xFFCC88FF;
        };
    }

    public int getNameColor() {
        return switch (this) {
            case NONE -> 0xAAAAAA;
            case WHITE_GRAY -> 0xD0D0D0;
            case GREEN_WHITE -> 0x55FF55;
            case CYAN_WHITE -> 0x55FFFF;
            case BLUE_WHITE -> 0x9B9BFF;
            case RAINBOW -> 0xFFBF00;
            case BLACK_WHITE -> 0xCC7FCC;
            case RED_BLACK -> 0xFF6666;
            case DARK_RED_PURPLE -> 0xCC6699;
            case GOLD_BLACK -> 0xFFCC00;
            case CUSTOM_GRADIENT -> 0xCC88FF;
        };
    }

    public int getBadgeBgColor() {
        return switch (this) {
            case NONE -> 0x40222222;
            case WHITE_GRAY -> 0x40333333;
            case GREEN_WHITE -> 0x40225522;
            case CYAN_WHITE -> 0x40225555;
            case BLUE_WHITE -> 0x40222255;
            case RAINBOW -> 0x50553300;
            case BLACK_WHITE -> 0x40333333;
            case RED_BLACK -> 0x50552222;
            case DARK_RED_PURPLE -> 0x50220022;
            case GOLD_BLACK -> 0x50554400;
            case CUSTOM_GRADIENT -> 0x50220044;
        };
    }

    public int getBadgeBorderColor() {
        return switch (this) {
            case NONE -> 0x60555555;
            case WHITE_GRAY -> 0x70888888;
            case GREEN_WHITE -> 0x7055FF55;
            case CYAN_WHITE -> 0x7055FFFF;
            case BLUE_WHITE -> 0x705555FF;
            case RAINBOW -> 0x80FFAA00;
            case BLACK_WHITE -> 0x70AAAAAA;
            case RED_BLACK -> 0x80FF5555;
            case DARK_RED_PURPLE -> 0x80CC6699;
            case GOLD_BLACK -> 0x80FFCC00;
            case CUSTOM_GRADIENT -> 0x80CC88FF;
        };
    }

    public int getFrameColor() {
        return switch (this) {
            case NONE -> 0xFF888888;
            case WHITE_GRAY -> 0xFFBBBBBB;
            case GREEN_WHITE -> 0xFF55FF55;
            case CYAN_WHITE -> 0xFF55FFFF;
            case BLUE_WHITE -> 0xFF5555FF;
            case RAINBOW -> 0xFFFFAA00;
            case BLACK_WHITE -> 0xFFAAAAAA;
            case RED_BLACK -> 0xFFFF5555;
            case DARK_RED_PURPLE -> 0xFFCC6699;
            case GOLD_BLACK -> 0xFFFFCC00;
            case CUSTOM_GRADIENT -> 0xFFCC88FF;
        };
    }

    public int getPanelColor() {
        return switch (this) {
            case NONE -> 0x40000000;
            case WHITE_GRAY -> 0x40222222;
            case GREEN_WHITE -> 0x40112211;
            case CYAN_WHITE -> 0x40112222;
            case BLUE_WHITE -> 0x40111122;
            case RAINBOW -> 0x50221100;
            case BLACK_WHITE -> 0x50111111;
            case RED_BLACK -> 0x50221111;
            case DARK_RED_PURPLE -> 0x50220022;
            case GOLD_BLACK -> 0x50221100;
            case CUSTOM_GRADIENT -> 0x50220033;
        };
    }

    public int getAccentColor() {
        return switch (this) {
            case NONE -> 0xAAAAAA;
            case WHITE_GRAY -> 0xAAAAAA;
            case GREEN_WHITE -> 0x55FF55;
            case CYAN_WHITE -> 0x55FFFF;
            case BLUE_WHITE -> 0x5555FF;
            case RAINBOW -> 0xFFAA00;
            case BLACK_WHITE -> 0xFFFFFF;
            case RED_BLACK -> 0xFF5555;
            case DARK_RED_PURPLE -> 0xAA00AA;
            case GOLD_BLACK -> 0xFFCC00;
            case CUSTOM_GRADIENT -> 0xCC88FF;
        };
    }

    public static ChromaType fromString(String name) {
        if (name == null || name.isEmpty()) return NONE;
        try {
            return valueOf(name.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
