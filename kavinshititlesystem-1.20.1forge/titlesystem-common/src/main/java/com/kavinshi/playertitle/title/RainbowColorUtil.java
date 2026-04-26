package com.kavinshi.playertitle.title;

public final class RainbowColorUtil {
    private RainbowColorUtil() {}

    private static volatile long cachedTimeMillis = System.currentTimeMillis();

    public static void updateCachedTime() {
        cachedTimeMillis = System.currentTimeMillis();
    }

    public static long getCachedTimeMillis() {
        return cachedTimeMillis;
    }

    public static int getChromaColorForChar(ChromaType type, int charIndex, int totalChars) {
        return getChromaColorForChar(type, charIndex, totalChars, cachedTimeMillis);
    }

    public static int getChromaColorForChar(ChromaType type, int charIndex, int totalChars, long timeMillis) {
        return switch (type) {
            case RAINBOW -> getRainbowColorForChar(charIndex, totalChars, timeMillis);
            case WHITE_GRAY -> getWhiteGrayChroma(charIndex, totalChars, timeMillis);
            case GREEN_WHITE -> getGreenWhiteChroma(charIndex, totalChars, timeMillis);
            case CYAN_WHITE -> getCyanWhiteChroma(charIndex, totalChars, timeMillis);
            case BLUE_WHITE -> getBlueWhiteChroma(charIndex, totalChars, timeMillis);
            case BLACK_WHITE -> getBlackWhiteChroma(charIndex, totalChars, timeMillis);
            case RED_BLACK -> getRedBlackChroma(charIndex, totalChars, timeMillis);
            case DARK_RED_PURPLE -> getDarkRedPurpleChroma(charIndex, totalChars, timeMillis);
            case GOLD_BLACK -> getGoldBlackChroma(charIndex, totalChars, timeMillis);
            case CUSTOM_GRADIENT -> 0xFFFFFF;
            default -> 0xFFFFFF;
        };
    }

    public static int getGradientColorForChar(int color1, int color2, int charIndex, int totalChars) {
        return getGradientColorForChar(color1, color2, charIndex, totalChars, cachedTimeMillis);
    }

    public static int getGradientColorForChar(int color1, int color2, int charIndex, int totalChars, long timeMillis) {
        float time = (float) (timeMillis % 2000L) / 2000.0f;
        float charOffset = (float) charIndex / Math.max(totalChars, 1) * 0.5f;
        float blend = (float) (Math.sin((time + charOffset) * Math.PI * 2.0) * 0.5 + 0.5);

        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * blend);
        int g = (int) (g1 + (g2 - g1) * blend);
        int b = (int) (b1 + (b2 - b1) * blend);
        return r << 16 | g << 8 | b;
    }

    public static int getRainbowColorForChar(int charIndex, int totalChars) {
        return getRainbowColorForChar(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getRainbowColorForChar(int charIndex, int totalChars, long timeMillis) {
        float offset = (float) charIndex / Math.max(totalChars, 1) * 0.5f;
        float hue = ((float) (timeMillis % 1500L) / 1500.0f + offset) % 1.0f;
        return hsbToRgb(hue, 1.0f, 1.0f);
    }

    public static int getWhiteGrayChroma(int charIndex, int totalChars) {
        return getWhiteGrayChroma(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getWhiteGrayChroma(int charIndex, int totalChars, long timeMillis) {
        float blend = getBlend(charIndex, totalChars, timeMillis);
        int gray = (int) (187.0f + 68.0f * blend);
        return gray << 16 | gray << 8 | gray;
    }

    public static int getGreenWhiteChroma(int charIndex, int totalChars) {
        return getGreenWhiteChroma(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getGreenWhiteChroma(int charIndex, int totalChars, long timeMillis) {
        float blend = getBlend(charIndex, totalChars, timeMillis);
        int r = (int) (85.0f + 170.0f * blend);
        return r << 16 | 255 << 8 | r;
    }

    public static int getCyanWhiteChroma(int charIndex, int totalChars) {
        return getCyanWhiteChroma(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getCyanWhiteChroma(int charIndex, int totalChars, long timeMillis) {
        float blend = getBlend(charIndex, totalChars, timeMillis);
        int r = (int) (85.0f + 170.0f * blend);
        return r << 16 | 255 << 8 | 255;
    }

    public static int getBlueWhiteChroma(int charIndex, int totalChars) {
        return getBlueWhiteChroma(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getBlueWhiteChroma(int charIndex, int totalChars, long timeMillis) {
        float blend = getBlend(charIndex, totalChars, timeMillis);
        int r = (int) (85.0f + 170.0f * blend);
        return r << 16 | r << 8 | 255;
    }

    public static int getBlackWhiteChroma(int charIndex, int totalChars) {
        return getBlackWhiteChroma(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getBlackWhiteChroma(int charIndex, int totalChars, long timeMillis) {
        float blend = getBlend(charIndex, totalChars, timeMillis);
        int gray = (int) (17.0f + 238.0f * blend);
        return gray << 16 | gray << 8 | gray;
    }

    public static int getRedBlackChroma(int charIndex, int totalChars) {
        return getRedBlackChroma(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getRedBlackChroma(int charIndex, int totalChars, long timeMillis) {
        float blend = getBlend(charIndex, totalChars, timeMillis);
        int r = (int) (51.0f + 204.0f * blend);
        return r << 16;
    }

    public static int getDarkRedPurpleChroma(int charIndex, int totalChars) {
        return getDarkRedPurpleChroma(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getDarkRedPurpleChroma(int charIndex, int totalChars, long timeMillis) {
        float blend = getBlend(charIndex, totalChars, timeMillis);
        int r = (int) (128.0f + 11.0f * blend);
        int b = (int) (128.0f * (1.0f - blend));
        return r << 16 | b;
    }

    public static int getGoldBlackChroma(int charIndex, int totalChars) {
        return getGoldBlackChroma(charIndex, totalChars, cachedTimeMillis);
    }

    public static int getGoldBlackChroma(int charIndex, int totalChars, long timeMillis) {
        float blend = getBlend(charIndex, totalChars, timeMillis);
        int r = (int) (17.0f + 238.0f * blend);
        int g = (int) (17.0f + 198.0f * blend);
        int b = (int) (17.0f * (1.0f - blend));
        return r << 16 | g << 8 | b;
    }

    private static float getBlend(int charIndex, int totalChars) {
        return getBlend(charIndex, totalChars, cachedTimeMillis);
    }

    private static float getBlend(int charIndex, int totalChars, long timeMillis) {
        float time = (float) (timeMillis % 1500L) / 1500.0f;
        float charOffset = (float) charIndex / Math.max(totalChars, 1) * 0.3f;
        return (float) (Math.sin((time + charOffset) * Math.PI * 2.0) * 0.5 + 0.5);
    }

    public static int hsbToRgb(float hue, float saturation, float brightness) {
        if (saturation == 0.0f) {
            int v = (int) (brightness * 255.0f + 0.5f);
            return v << 16 | v << 8 | v;
        }
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1.0f - saturation);
        float q = brightness * (1.0f - saturation * f);
        float t = brightness * (1.0f - saturation * (1.0f - f));
        int r, g, b;
        switch ((int) h) {
            case 0 -> { r = (int)(brightness*255+0.5f); g = (int)(t*255+0.5f); b = (int)(p*255+0.5f); }
            case 1 -> { r = (int)(q*255+0.5f); g = (int)(brightness*255+0.5f); b = (int)(p*255+0.5f); }
            case 2 -> { r = (int)(p*255+0.5f); g = (int)(brightness*255+0.5f); b = (int)(t*255+0.5f); }
            case 3 -> { r = (int)(p*255+0.5f); g = (int)(q*255+0.5f); b = (int)(brightness*255+0.5f); }
            case 4 -> { r = (int)(t*255+0.5f); g = (int)(p*255+0.5f); b = (int)(brightness*255+0.5f); }
            default -> { r = (int)(brightness*255+0.5f); g = (int)(p*255+0.5f); b = (int)(q*255+0.5f); }
        }
        return r << 16 | g << 8 | b;
    }
}
