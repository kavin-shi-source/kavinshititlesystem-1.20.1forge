package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class BuffHandler {

    private static final long UUID_BASE_MSB = 0x00000000_0000_0000L;
    private static final long UUID_BASE_LSB = 0x0000_0000_0000_0001L;

    public static void applyBuffs(Player player, int titleId) {
        TitleRegistry registry = com.kavinshi.playertitle.bootstrap.RewriteBootstrap.getInstance().getTitleRegistry();
        TitleDefinition title = registry.getTitle(titleId);
        if (title == null) return;

        for (int i = 0; i < title.getBuffs().size(); i++) {
            var buff = title.getBuffs().get(i);
            Attribute attribute = getAttribute(buff.getType());
            if (attribute == null) continue;

            UUID modifierUuid = new UUID(UUID_BASE_MSB, UUID_BASE_LSB + ((long) titleId << 16) + i);
            AttributeModifier.Operation op = buff.getType() == com.kavinshi.playertitle.title.TitleBuff.BuffType.SPEED
                    ? AttributeModifier.Operation.MULTIPLY_BASE
                    : AttributeModifier.Operation.ADDITION;
            AttributeModifier modifier = new AttributeModifier(
                    modifierUuid, "playertitle.buff." + titleId + "." + i, buff.getValue(), op);
            player.getAttribute(attribute).addPermanentModifier(modifier);
        }
    }

    public static void removeBuffs(Player player, int titleId) {
        TitleRegistry registry = com.kavinshi.playertitle.bootstrap.RewriteBootstrap.getInstance().getTitleRegistry();
        TitleDefinition title = registry.getTitle(titleId);
        if (title == null) return;

        for (int i = 0; i < title.getBuffs().size(); i++) {
            var buff = title.getBuffs().get(i);
            Attribute attribute = getAttribute(buff.getType());
            if (attribute == null) continue;

            UUID modifierUuid = new UUID(UUID_BASE_MSB, UUID_BASE_LSB + ((long) titleId << 16) + i);
            player.getAttribute(attribute).removeModifier(modifierUuid);
        }
    }

    private static Attribute getAttribute(com.kavinshi.playertitle.title.TitleBuff.BuffType type) {
        return switch (type) {
            case ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case MAX_HEALTH -> Attributes.MAX_HEALTH;
            case SPEED -> Attributes.MOVEMENT_SPEED;
            case ARMOR -> Attributes.ARMOR;
            case ARMOR_TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
            case LUCK -> Attributes.LUCK;
            default -> null;
        };
    }
}
