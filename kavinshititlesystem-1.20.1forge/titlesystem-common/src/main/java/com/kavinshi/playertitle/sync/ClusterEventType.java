package com.kavinshi.playertitle.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ClusterEventType {
    TITLE_ASSIGNED,
    TITLE_REMOVED,
    TITLE_UPDATED,
    TITLE_PROGRESS_UPDATED,
    TITLE_EQUIP_STATE_CHANGED,
    SERVER_ANNOUNCEMENT;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterEventType.class);
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .addModule(new ParameterNamesModule())
        .addModule(new JavaTimeModule())
        .build();

    public Class<? extends ClusterSyncEvent> getEventClass() {
        return switch (this) {
            case TITLE_ASSIGNED -> TitleAssignedEvent.class;
            case TITLE_REMOVED -> TitleRemovedEvent.class;
            case TITLE_UPDATED -> TitleUpdatedEvent.class;
            case TITLE_PROGRESS_UPDATED -> TitleProgressUpdatedEvent.class;
            case TITLE_EQUIP_STATE_CHANGED -> TitleEquipStateChangedEvent.class;
            case SERVER_ANNOUNCEMENT -> ServerAnnouncementEvent.class;
        };
    }

    public static ClusterSyncEvent deserialize(String payload) {
        try {
            var root = OBJECT_MAPPER.readTree(payload);
            var eventTypeNode = root.get("eventType");
            if (eventTypeNode == null || eventTypeNode.isNull()) {
                LOGGER.warn("Missing 'eventType' field in cluster event payload");
                return null;
            }
            String eventTypeStr = eventTypeNode.asText();
            ClusterEventType eventType = ClusterEventType.valueOf(eventTypeStr);
            return OBJECT_MAPPER.readValue(payload, eventType.getEventClass());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown event type in payload: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize cluster event: {}", e.getMessage());
            return null;
        }
    }
}