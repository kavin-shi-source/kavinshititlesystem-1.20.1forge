package com.kavinshi.playertitle.client;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("playertitleclient")
public final class PlayerTitleClientMod {
    public static final String MOD_ID = "playertitleclient";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlayerTitleClientMod() {
        LOGGER.info("Initializing PlayerTitle Client Module");
    }
}