package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.UUID;

public class ClusterSyncPacket extends AbstractPacket {
    private static final Logger LOGGER = LogManager.getLogger(ClusterSyncPacket.class);

    private final String sourceServer;
    private final String eventType;
    private final UUID playerId;
    private final long revision;
    private final long timestampMs;
    private final String payload;

    public ClusterSyncPacket(String sourceServer, String eventType, UUID playerId,
                             long revision, long timestampMs, String payload) {
        this.sourceServer = sourceServer;
        this.eventType = eventType;
        this.playerId = playerId;
        this.revision = revision;
        this.timestampMs = timestampMs;
        this.payload = payload;
    }

    public ClusterSyncPacket(FriendlyByteBuf buffer) {
        this.sourceServer = readString(buffer);
        this.eventType = readString(buffer);
        this.playerId = readUUID(buffer);
        this.revision = buffer.readLong();
        this.timestampMs = buffer.readLong();
        this.payload = readString(buffer);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        writeString(buffer, sourceServer);
        writeString(buffer, eventType);
        writeUUID(buffer, playerId);
        buffer.writeLong(revision);
        buffer.writeLong(timestampMs);
        writeString(buffer, payload);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            try {
                PacketHandlers.handleClusterSync(sourceServer, eventType, playerId, revision, timestampMs, payload);
            } catch (Exception e) {
                LOGGER.error("Error handling ClusterSyncPacket", e);
            }
        });
        context.setPacketHandled(true);
    }

    public String getSourceServer() { return sourceServer; }
    public String getEventType() { return eventType; }
    public UUID getPlayerId() { return playerId; }
    public long getRevision() { return revision; }
    public long getTimestampMs() { return timestampMs; }
    public String getPayload() { return payload; }
}
