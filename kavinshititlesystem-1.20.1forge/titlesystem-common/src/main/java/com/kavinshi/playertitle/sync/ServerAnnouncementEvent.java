package com.kavinshi.playertitle.sync;

import java.time.Instant;
import java.util.UUID;

public class ServerAnnouncementEvent extends ClusterSyncEvent {
    private final String message;
    private final String sourceServer;

    public ServerAnnouncementEvent(UUID playerId, long revision, String message, String sourceServer) {
        super(playerId, revision, ClusterEventType.SERVER_ANNOUNCEMENT);
        this.message = message != null ? message : "";
        this.sourceServer = sourceServer != null ? sourceServer : "unknown";
        validate();
    }

    public ServerAnnouncementEvent(UUID eventId, UUID playerId, long revision, Instant timestamp,
                                   String message, String sourceServer) {
        super(eventId, playerId, revision, timestamp, ClusterEventType.SERVER_ANNOUNCEMENT);
        this.message = message != null ? message : "";
        this.sourceServer = sourceServer != null ? sourceServer : "unknown";
        validate();
    }

    public String getMessage() {
        return message;
    }

    public String getSourceServer() {
        return sourceServer;
    }

    @Override
    public void validate() {
        super.validate();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message cannot be empty");
        }
    }

    @Override
    public String toString() {
        return String.format("ServerAnnouncementEvent{eventId=%s, playerId=%s, revision=%d, sourceServer=%s, message=%s}",
            getEventId(), getPlayerId(), getRevision(), sourceServer, message);
    }
}
