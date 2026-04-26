package com.kavinshi.playertitle.title;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public final class TitleParseUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TitleParseUtils.class);
    private TitleParseUtils() {}

    public static TitleStyleMode safeStyleMode(String raw) {
        if (raw == null) return TitleStyleMode.PLAIN;
        try {
            return TitleStyleMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown TitleStyleMode '{}', falling back to PLAIN", raw);
            return TitleStyleMode.PLAIN;
        }
    }

    public static TitleConditionType safeConditionType(String raw) {
        if (raw == null) return TitleConditionType.KILL_ANY_HOSTILE;
        try {
            return TitleConditionType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown TitleConditionType '{}', falling back to KILL_ANY_HOSTILE", raw);
            return TitleConditionType.KILL_ANY_HOSTILE;
        }
    }

    public static TitleBuff.BuffType safeBuffType(String raw) {
        if (raw == null) return TitleBuff.BuffType.ATTACK_DAMAGE;
        try {
            return TitleBuff.BuffType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown TitleBuff.BuffType '{}', falling back to ATTACK_DAMAGE", raw);
            return TitleBuff.BuffType.ATTACK_DAMAGE;
        }
    }
}
