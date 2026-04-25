package com.kavinshi.playertitle.util;

import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TabModDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TabModDetector.class);
    private static Boolean cached = null;

    private static final String[] TAB_MOD_IDS = {
        "tab", "bettertab", "tabby", "vanillatweaks", "vt",
        "essential", "velocity", "velocitytab", "vtab", "vtabmod",
        "fabrictab", "playerlist", "playerlistmod", "tablist"
    };

    private TabModDetector() {}

    public static boolean hasTabMod() {
        if (cached == null) {
            for (String modId : TAB_MOD_IDS) {
                if (ModList.get().isLoaded(modId)) {
                    cached = true;
                    LOGGER.info("Tab plugin detected: {}, skipping custom tab list modifications", modId);
                    return cached;
                }
            }
            cached = false;
        }
        return cached;
    }

    public static void invalidateCache() {
        cached = null;
    }
}
