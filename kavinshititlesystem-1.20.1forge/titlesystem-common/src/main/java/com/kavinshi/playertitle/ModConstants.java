package com.kavinshi.playertitle;

/**
 * 模组常量定义
 * 这些常量与gradle.properties中的属性对应
 */
public class ModConstants {
    public static final String MOD_ID = "playertitle";
    public static final String MOD_NAME = "Kavinshi's Player Title System";
    public static final String MOD_AUTHORS = "Kavinshi";
    public static final String MOD_DESCRIPTION = "A player title system with cross-server synchronization";

    public static final int NO_TITLE_EQUIPPED = -1;
    public static final int CUSTOM_TITLE_ID = -2;
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    public static final int DEFAULT_GRAY = 0xAAAAAA;
    public static final int TICKS_PER_MINUTE = 20 * 60;
    public static final long MILLIS_PER_SECOND = 1000L;
    public static final int MAX_STRING_LENGTH = 32767;

    public static final String CHANNEL_MAIN = MOD_ID + ":main";
    public static final String CHANNEL_DATA = MOD_ID + ":data";
    public static final String CHANNEL_CHAT = MOD_ID + ":chat";

    public static String getModVersion() {
        try {
            return net.minecraftforge.fml.ModList.get()
                .getModContainerById(MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
        } catch (Throwable t) {
            return "unknown";
        }
    }
    
    private ModConstants() {
        // 防止实例化
    }
}