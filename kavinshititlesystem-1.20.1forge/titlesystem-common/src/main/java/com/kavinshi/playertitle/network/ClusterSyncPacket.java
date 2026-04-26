package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ClusterSyncPacket extends AbstractPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSyncPacket.class);
    private static final int MAX_PAYLOAD_SIZE = 65536;

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
        this.payload = truncatePayload(payload);
    }

    public ClusterSyncPacket(FriendlyByteBuf buffer) {
        this.sourceServer = readString(buffer);
        this.eventType = readString(buffer);
        this.playerId = readUUID(buffer);
        this.revision = buffer.readLong();
        this.timestampMs = buffer.readLong();
        this.payload = truncatePayload(readString(buffer));
    }

    private static String truncatePayload(String payload) {
        if (payload == null) return "";
        byte[] bytes = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= MAX_PAYLOAD_SIZE) return payload;
        LOGGER.warn("ClusterSyncPacket payload too large ({} bytes), truncating to {} bytes",
            bytes.length, MAX_PAYLOAD_SIZE);
        byte[] truncated = new byte[MAX_PAYLOAD_SIZE];
        System.arraycopy(bytes, 0, truncated, 0, MAX_PAYLOAD_SIZE);
        int end = MAX_PAYLOAD_SIZE;
        while (end > 0 && (truncated[end - 1] & 0xC0) == 0x80) end--;
        if (end > 0 && (truncated[end - 1] & 0x80) != 0) end--;
        return new String(truncated, 0, end, java.nio.charset.StandardCharsets.UTF_8);
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
        try {
            PacketHandlers.handleClusterSync(sourceServer, eventType, playerId, revision, timestampMs, payload);
        } catch (Exception e) {
            LOGGER.error("Error handling ClusterSyncPacket", e);
        }
    }

    public String getSourceServer() { return sourceServer; }
    public String getEventType() { return eventType; }
    public UUID getPlayerId() { return playerId; }
    public long getRevision() { return revision; }
    public long getTimestampMs() { return timestampMs; }
    public String getPayload() { return payload; }
}
