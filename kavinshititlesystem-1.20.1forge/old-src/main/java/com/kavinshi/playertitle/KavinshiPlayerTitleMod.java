package com.kavinshi.playertitle;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(KavinshiPlayerTitleMod.MOD_ID)
public final class KavinshiPlayerTitleMod {
    public static final String MOD_ID = "playertitle";
    public static final String MOD_NAME = "kavinshi's playertitle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KavinshiPlayerTitleMod() {
        RewriteBootstrap.initialize();
        LOGGER.info("Initializing {}", MOD_NAME);
    }
}
