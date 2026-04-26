package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@SuppressWarnings("null")
public abstract class AbstractPacket {
    
    public abstract void encode(FriendlyByteBuf buffer);
    
    public abstract void handle(NetworkEvent.Context context);
    
    protected static void writeString(FriendlyByteBuf buffer, String str) {
        buffer.writeUtf(str, 32767);
    }
    
    protected static String readString(FriendlyByteBuf buffer) {
        return buffer.readUtf(32767);
    }
    
    protected static void writeUUID(FriendlyByteBuf buffer, java.util.UUID uuid) {
        buffer.writeUUID(uuid);
    }
    
    protected static java.util.UUID readUUID(FriendlyByteBuf buffer) {
        return buffer.readUUID();
    }
}