package com.kavinshi.playertitle.network;

import com.kavinshi.playertitle.title.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步标题注册表数据包，用于在客户端和服务器之间同步标题定义数据。
 * 包含所有已注册标题的完整定义信息，包括条件、增益效果等。
 */
public class SyncTitleRegistryPacket extends AbstractPacket {

    private final List<TitleDefinition> titles;

    public SyncTitleRegistryPacket(List<TitleDefinition> titles) {
        this.titles = titles;
    }

    public SyncTitleRegistryPacket(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        this.titles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            titles.add(readTitleDefinition(buffer));
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(titles.size());
        for (TitleDefinition title : titles) {
            writeTitleDefinition(buffer, title);
        }
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        PacketHandlers.handleSyncTitleRegistry(titles);
    }

    private static void writeTitleDefinition(FriendlyByteBuf buffer, TitleDefinition title) {
        buffer.writeVarInt(title.getId());
        writeString(buffer, title.getName());
        buffer.writeVarInt(title.getDisplayOrder());
        buffer.writeInt(title.getColor());
        writeString(buffer, title.getChromaType());
        writeString(buffer, title.getDescription());
        writeString(buffer, title.getCategory());
        writeString(buffer, title.getIcon());
        writeString(buffer, title.getIconColor());

        writeString(buffer, title.getStyleMode().name());

        List<String> baseColors = title.getBaseColors();
        buffer.writeVarInt(baseColors.size());
        for (String color : baseColors) {
            writeString(buffer, color);
        }

        TitleAnimationProfile anim = title.getAnimationProfile();
        buffer.writeBoolean(anim != null);
        if (anim != null) {
            buffer.writeVarInt(anim.cycleMillis());
            buffer.writeVarInt(anim.stepSize());
        }

        writeString(buffer, title.getAuraEffect() != null ? title.getAuraEffect() : "");

        List<TitleCondition> conditions = title.getConditions();
        buffer.writeVarInt(conditions.size());
        for (TitleCondition cond : conditions) {
            writeString(buffer, cond.getType().name());
            writeString(buffer, cond.getTarget());
            buffer.writeVarInt(cond.getRequiredCount());
        }

        List<TitleBuff> buffs = title.getBuffs();
        buffer.writeVarInt(buffs.size());
        for (TitleBuff buff : buffs) {
            writeString(buffer, buff.getType().name());
            buffer.writeDouble(buff.getValue());
            writeString(buffer, buff.getTarget());
        }
    }

    private static TitleDefinition readTitleDefinition(FriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        String name = readString(buffer);
        int displayOrder = buffer.readVarInt();
        int color = buffer.readInt();
        String chromaType = readString(buffer);
        String description = readString(buffer);
        String category = readString(buffer);
        String icon = readString(buffer);
        String iconColor = readString(buffer);

        TitleStyleMode styleMode = TitleParseUtils.safeStyleMode(readString(buffer));

        int baseColorCount = buffer.readVarInt();
        List<String> baseColors = new ArrayList<>(baseColorCount);
        for (int i = 0; i < baseColorCount; i++) {
            baseColors.add(readString(buffer));
        }

        TitleAnimationProfile animationProfile = null;
        if (buffer.readBoolean()) {
            animationProfile = new TitleAnimationProfile(buffer.readVarInt(), buffer.readVarInt());
        }

        String auraEffect = readString(buffer);
        if (auraEffect.isEmpty()) auraEffect = null;

        int condCount = buffer.readVarInt();
        List<TitleCondition> conditions = new ArrayList<>(condCount);
        for (int i = 0; i < condCount; i++) {
            TitleConditionType type = TitleParseUtils.safeConditionType(readString(buffer));
            String target = readString(buffer);
            int requiredCount = buffer.readVarInt();
            if (type != null) {
                conditions.add(new TitleCondition(type, target, requiredCount));
            }
        }

        int buffCount = buffer.readVarInt();
        List<TitleBuff> buffs = new ArrayList<>(buffCount);
        for (int i = 0; i < buffCount; i++) {
            TitleBuff.BuffType type = TitleParseUtils.safeBuffType(readString(buffer));
            double value = buffer.readDouble();
            String target = readString(buffer);
            if (type != null) {
                buffs.add(new TitleBuff(type, value, target));
            }
        }

        return new TitleDefinition(id, name, displayOrder, color, chromaType, conditions,
            buffs, description, category, icon, iconColor,
            styleMode, baseColors, animationProfile, auraEffect);
    }

    public List<TitleDefinition> getTitles() {
        return titles;
    }
}
