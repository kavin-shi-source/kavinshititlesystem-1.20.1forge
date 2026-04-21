package com.kavinshi.playertitle.network;

import com.kavinshi.playertitle.title.TitleBuff;
import com.kavinshi.playertitle.title.TitleCondition;
import com.kavinshi.playertitle.title.TitleConditionType;
import com.kavinshi.playertitle.title.TitleDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步标题注册表数据包，用于在客户端和服务器之间同步标题定义数据。
 * 包含所有已注册标题的完整定义信息，包括条件、增益效果等。
 */
public class SyncTitleRegistryPacket extends AbstractPacket {
    private static final Logger LOGGER = LogManager.getLogger(SyncTitleRegistryPacket.class);

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

        int condCount = buffer.readVarInt();
        List<TitleCondition> conditions = new ArrayList<>(condCount);
        for (int i = 0; i < condCount; i++) {
            TitleConditionType type = TitleConditionType.valueOf(readString(buffer));
            String target = readString(buffer);
            int requiredCount = buffer.readVarInt();
            conditions.add(new TitleCondition(type, target, requiredCount));
        }

        int buffCount = buffer.readVarInt();
        List<TitleBuff> buffs = new ArrayList<>(buffCount);
        for (int i = 0; i < buffCount; i++) {
            TitleBuff.BuffType type = TitleBuff.BuffType.valueOf(readString(buffer));
            double value = buffer.readDouble();
            String target = readString(buffer);
            buffs.add(new TitleBuff(type, value, target));
        }

        return new TitleDefinition(id, name, displayOrder, color, chromaType, conditions,
            buffs, description, category, icon, iconColor,
            com.kavinshi.playertitle.title.TitleStyleMode.PLAIN, List.of(), null, null);
    }

    public List<TitleDefinition> getTitles() {
        return titles;
    }
}
