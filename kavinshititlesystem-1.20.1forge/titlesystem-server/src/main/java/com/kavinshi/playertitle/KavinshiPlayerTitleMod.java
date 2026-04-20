package com.kavinshi.playertitle;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("playertitleserver")
public final class KavinshiPlayerTitleMod {
    public static final String MOD_ID = "playertitleserver";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KavinshiPlayerTitleMod() {
        RewriteBootstrap.initialize();
        LOGGER.info("Initializing PlayerTitle Server Module");
    }
}
