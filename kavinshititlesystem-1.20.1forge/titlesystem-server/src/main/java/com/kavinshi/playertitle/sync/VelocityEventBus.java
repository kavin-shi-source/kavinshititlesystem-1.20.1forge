package com.kavinshi.playertitle.sync;

import com.kavinshi.playertitle.network.ClusterSyncPacket;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

public class VelocityEventBus implements ClusterEventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(VelocityEventBus.class);

    private final Map<ClusterEventType, Set<EventListener>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final String channelName;
    private final String serverName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VelocityEventBus(String channelName, String serverName) {
        this.channelName = channelName != null ? channelName : "playertitle:cluster";
        this.serverName = serverName != null ? serverName : "unknown";
    }

    @Override
    public void publish(ClusterSyncEvent event) throws EventBusException {
        if (!running.get()) {
            throw new EventBusException("VelocityEventBus is not running");
        }

        try {
            String payload = objectMapper.writeValueAsString(event);

            ClusterSyncPacket packet = new ClusterSyncPacket(
                serverName,
                event.getEventType().name(),
                event.getPlayerId(),
                event.getRevision(),
                event.getTimestamp().toEpochMilli(),
                payload
            );

            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.warn("Cannot publish - server instance is null");
                return;
            }

            var playerList = server.getPlayerList().getPlayers();
            if (playerList.isEmpty()) {
                LOGGER.warn("Cannot publish - no online players to carry the message");
                return;
            }

            ServerPlayer carrier = playerList.get(0);
            NetworkHandler.getChannel().send(PacketDistributor.PLAYER.with(() -> carrier), packet);

            LOGGER.debug("Published {} via player {}", event.getEventType(), carrier.getName().getString());
        } catch (Exception e) {
            throw new EventBusException("Failed to publish event via Velocity plugin message", e);
        }
    }

    public void onClusterSyncPacket(ClusterSyncPacket packet) {
        if (!running.get()) {
            return;
        }

        if (serverName.equals(packet.getSourceServer())) {
            return;
        }

        try {
            ClusterEventType eventType = ClusterEventType.valueOf(packet.getEventType());

            ClusterSyncEvent event = deserializeEvent(eventType, packet.getPayload());
            if (event == null) {
                LOGGER.warn("Failed to deserialize event payload");
                return;
            }

            Set<EventListener> typeListeners = listeners.get(eventType);
            if (typeListeners != null) {
                for (EventListener listener : typeListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        LOGGER.error("Error in listener: {}", e.getMessage());
                    }
                }
            }

            Set<EventListener> globalListeners = listeners.get(null);
            if (globalListeners != null) {
                for (EventListener listener : globalListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        LOGGER.error("Error in global listener: {}", e.getMessage());
                    }
                }
            }

            LOGGER.debug("Processed {} from {}", eventType, packet.getSourceServer());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown event type: {}", packet.getEventType());
        }
    }

    private ClusterSyncEvent deserializeEvent(ClusterEventType eventType, String payload) {
        try {
            return switch (eventType) {
                case TITLE_ASSIGNED -> objectMapper.readValue(payload, TitleAssignedEvent.class);
                case TITLE_REMOVED -> objectMapper.readValue(payload, TitleRemovedEvent.class);
                case TITLE_UPDATED -> objectMapper.readValue(payload, TitleUpdatedEvent.class);
                case TITLE_PROGRESS_UPDATED -> objectMapper.readValue(payload, TitleProgressUpdatedEvent.class);
                case TITLE_EQUIP_STATE_CHANGED -> objectMapper.readValue(payload, TitleEquipStateChangedEvent.class);
                case SERVER_ANNOUNCEMENT -> objectMapper.readValue(payload, ServerAnnouncementEvent.class);
            };
        } catch (Exception e) {
            LOGGER.error("Deserialization error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void subscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (listener == null) throw new EventBusException("listener cannot be null");
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(listener);
    }

    @Override
    public void subscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) throw new EventBusException("listener cannot be null");
        for (ClusterEventType eventType : ClusterEventType.values()) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(listener);
        }
    }

    @Override
    public void unsubscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (eventType == null || listener == null) return;
        Set<EventListener> set = listeners.get(eventType);
        if (set != null) {
            set.remove(listener);
            if (set.isEmpty()) listeners.remove(eventType);
        }
    }

    @Override
    public void unsubscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) return;
        for (Map.Entry<ClusterEventType, Set<EventListener>> entry : listeners.entrySet()) {
            entry.getValue().remove(listener);
            if (entry.getValue().isEmpty()) listeners.remove(entry.getKey());
        }
    }

    @Override
    public void start() throws EventBusException {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("Started - serverName={}, channel={}", serverName, channelName);
        }
    }

    @Override
    public void stop() throws EventBusException {
        if (running.compareAndSet(true, false)) {
            listeners.clear();
            LOGGER.info("Stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public String getImplementationName() {
        return "Velocity";
    }

    public String getChannelName() {
        return channelName;
    }

    public String getServerName() {
        return serverName;
    }
}
