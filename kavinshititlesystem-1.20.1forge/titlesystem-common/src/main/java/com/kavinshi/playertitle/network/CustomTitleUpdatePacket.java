package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * 自定义标题更新数据包，用于同步玩家自定义标题的更改。
 * 支持更新自定义标题的文本、颜色、启用状态等。
 */
public class CustomTitleUpdatePacket extends AbstractPacket {
    private static final Logger LOGGER = LogManager.getLogger(CustomTitleUpdatePacket.class);

    public enum UpdateType {
        SET_TEXT(0), SET_COLOR1(1), SET_COLOR2(2), TOGGLE_USE(3);
        private final int id;
        UpdateType(int id) { this.id = id; }
        public int getId() { return id; }
        public static UpdateType fromId(int id) {
            for (UpdateType type : values()) if (type.id == id) return type;
            return SET_TEXT;
        }
    }

    private final UpdateType updateType;
    private final String text;
    private final int color1;
    private final int color2;
    private final boolean useCustom;

    public CustomTitleUpdatePacket(UpdateType updateType, String text) {
        this.updateType = updateType;
        this.text = text != null ? text : "";
        this.color1 = 0;
        this.color2 = 0;
        this.useCustom = false;
    }

    public CustomTitleUpdatePacket(UpdateType updateType, int color) {
        this.updateType = updateType;
        this.text = "";
        this.color1 = color;
        this.color2 = 0;
        this.useCustom = false;
    }

    public CustomTitleUpdatePacket(UpdateType updateType, int color1, int color2) {
        this.updateType = updateType;
        this.text = "";
        this.color1 = color1;
        this.color2 = color2;
        this.useCustom = false;
    }

    public CustomTitleUpdatePacket(UpdateType updateType, boolean useCustom) {
        this.updateType = updateType;
        this.text = "";
        this.color1 = 0;
        this.color2 = 0;
        this.useCustom = useCustom;
    }

    private CustomTitleUpdatePacket(UpdateType updateType, String text, int color1, int color2, boolean useCustom) {
        this.updateType = updateType;
        this.text = text != null ? text : "";
        this.color1 = color1;
        this.color2 = color2;
        this.useCustom = useCustom;
    }

    public CustomTitleUpdatePacket(FriendlyByteBuf buffer) {
        this.updateType = UpdateType.fromId(buffer.readVarInt());
        this.text = readString(buffer);
        this.color1 = buffer.readInt();
        this.color2 = buffer.readInt();
        this.useCustom = buffer.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(updateType.getId());
        writeString(buffer, text);
        buffer.writeInt(color1);
        buffer.writeInt(color2);
        buffer.writeBoolean(useCustom);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            PacketHandlers.handleCustomTitleUpdateServer(sender, updateType, text, color1, color2, useCustom);
        }
    }

    public UpdateType getUpdateType() { return updateType; }
    public String getText() { return text; }
    public int getColor1() { return color1; }
    public int getColor2() { return color2; }
    public boolean isUseCustom() { return useCustom; }
}