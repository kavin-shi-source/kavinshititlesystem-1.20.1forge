package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class HeadingRequestPacket extends AbstractPacket {

    private final UUID targetUuid;
    private final String heading;
    private final boolean isGrant;

    public HeadingRequestPacket(UUID targetUuid, String heading, boolean isGrant) {
        this.targetUuid = targetUuid;
        this.heading = heading != null ? heading : "";
        this.isGrant = isGrant;
    }

    public HeadingRequestPacket(FriendlyByteBuf buffer) {
        this.targetUuid = buffer.readUUID();
        this.heading = readString(buffer);
        this.isGrant = buffer.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(targetUuid);
        writeString(buffer, heading);
        buffer.writeBoolean(isGrant);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        // This is handled on the Proxy (Velocity) side, but we register it here to pass it through if needed.
        // Actually, the server should forward this to the proxy channel.
        context.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer sender = context.getSender();
            if (sender != null) {
                PacketHandlers.handleHeadingRequest(sender, targetUuid, heading, isGrant);
            }
        });
        context.setPacketHandled(true);
    }
}