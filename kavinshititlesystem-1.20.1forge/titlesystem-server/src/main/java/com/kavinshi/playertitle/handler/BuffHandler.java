package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.TitleBuff;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
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
            AttributeModifier.Operation op = buff.getType() == TitleBuff.BuffType.SPEED
                    ? AttributeModifier.Operation.MULTIPLY_BASE
                    : AttributeModifier.Operation.ADDITION;
            AttributeModifier modifier = new AttributeModifier(
                    modifierUuid, "playertitle.buff." + titleId + "." + i, buff.getValue(), op);
            var attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                attributeInstance.addPermanentModifier(modifier);
            }
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
            var attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(modifierUuid);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        TitleRegistry registry = com.kavinshi.playertitle.bootstrap.RewriteBootstrap.getInstance().getTitleRegistry();
        int equippedId = TitleCapability.get(player)
                .map(state -> state.getEquippedTitleId())
                .orElse(-1);
        if (equippedId < 0) return;

        TitleDefinition title = registry.getTitle(equippedId);
        if (title == null) return;

        List<TitleBuff> buffs = title.getBuffs();
        LivingEntity target = event.getEntity();
        String targetType = target.getType().getDescriptionId();

        for (TitleBuff buff : buffs) {
            if (buff.getType() == TitleBuff.BuffType.DAMAGE_MULTIPLIER && buff.appliesToTarget(targetType)) {
                float multiplier = (float) buff.getValue();
                float newDamage = event.getAmount() * multiplier;
                event.setAmount(newDamage);
            }
        }
    }

    private static Attribute getAttribute(TitleBuff.BuffType type) {
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
