package com.kavinshi.playertitle.network;

import com.kavinshi.playertitle.title.TitleDefinition;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PacketHandlers {
    private PacketHandlers() {}
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketHandlers.class);

    private enum HandlerKey {
        SYNC_PLAYER_TITLES,
        SYNC_TITLE_REGISTRY,
        TITLE_UPDATE_CLIENT,
        TITLE_UPDATE_SERVER,
        REQUEST_SYNC,
        CLUSTER_SYNC,
        HEADING_REQUEST
    }

    private static final ConcurrentHashMap<HandlerKey, Consumer<?>> handlers = new ConcurrentHashMap<>();

    public static void registerHeadingRequestHandler(Consumer<HeadingRequestContext> handler) {
        register(HandlerKey.HEADING_REQUEST, handler);
    }

    public static void registerClusterSyncHandler(Consumer<ClusterSyncContext> handler) {
        register(HandlerKey.CLUSTER_SYNC, handler);
    }

    public static void registerSyncPlayerTitlesHandler(Consumer<SyncPlayerTitlesContext> handler) {
        register(HandlerKey.SYNC_PLAYER_TITLES, handler);
    }

    public static void registerSyncTitleRegistryHandler(Consumer<SyncTitleRegistryContext> handler) {
        register(HandlerKey.SYNC_TITLE_REGISTRY, handler);
    }

    public static void registerTitleUpdateClientHandler(Consumer<TitleUpdateClientContext> handler) {
        register(HandlerKey.TITLE_UPDATE_CLIENT, handler);
    }

    public static void registerTitleUpdateServerHandler(Consumer<TitleUpdateServerContext> handler) {
        register(HandlerKey.TITLE_UPDATE_SERVER, handler);
    }

    public static void registerRequestSyncHandler(Consumer<RequestSyncContext> handler) {
        register(HandlerKey.REQUEST_SYNC, handler);
    }

    private static <T> void register(HandlerKey key, Consumer<T> handler) {
        Consumer<?> prev = handlers.putIfAbsent(key, handler);
        if (prev != null) {
            LOGGER.warn("Handler for {} was already registered, replacing", key);
            handlers.put(key, handler);
        }
    }

    public static void unregisterAll() {
        handlers.clear();
        LOGGER.info("All packet handlers unregistered");
    }

    @SuppressWarnings("unchecked")
    static void handleSyncPlayerTitles(UUID playerId, Set<Integer> unlockedTitleIds,
                                        int equippedTitleId, Map<String, Integer> killCounts,
                                        int aliveMinutes, String heading) {
        Consumer<SyncPlayerTitlesContext> h = (Consumer<SyncPlayerTitlesContext>) handlers.get(HandlerKey.SYNC_PLAYER_TITLES);
        if (h != null) {
            h.accept(new SyncPlayerTitlesContext(
                playerId, unlockedTitleIds, equippedTitleId, killCounts, aliveMinutes, heading));
        } else {
            LOGGER.warn("syncPlayerTitlesHandler not registered, dropping event for player {}", playerId);
        }
    }

    @SuppressWarnings("unchecked")
    static void handleSyncTitleRegistry(List<TitleDefinition> titles) {
        Consumer<SyncTitleRegistryContext> h = (Consumer<SyncTitleRegistryContext>) handlers.get(HandlerKey.SYNC_TITLE_REGISTRY);
        if (h != null) {
            h.accept(new SyncTitleRegistryContext(titles));
        } else {
            LOGGER.warn("syncTitleRegistryHandler not registered, dropping registry sync ({} titles)", titles.size());
        }
    }

    @SuppressWarnings("unchecked")
    static void handleTitleUpdateClient(TitleUpdatePacket.UpdateType updateType, UUID playerId,
                                         int titleId, String entityId, int count) {
        Consumer<TitleUpdateClientContext> h = (Consumer<TitleUpdateClientContext>) handlers.get(HandlerKey.TITLE_UPDATE_CLIENT);
        if (h != null) {
            h.accept(new TitleUpdateClientContext(updateType, playerId, titleId, entityId, count));
        } else {
            LOGGER.warn("titleUpdateClientHandler not registered, dropping {} event for player {}", updateType, playerId);
        }
    }

    @SuppressWarnings("unchecked")
    static void handleTitleUpdateServer(ServerPlayer sender, TitleUpdatePacket.UpdateType updateType,
                                         UUID playerId, int titleId) {
        Consumer<TitleUpdateServerContext> h = (Consumer<TitleUpdateServerContext>) handlers.get(HandlerKey.TITLE_UPDATE_SERVER);
        if (h != null) {
            h.accept(new TitleUpdateServerContext(sender, updateType, playerId, titleId));
        } else {
            LOGGER.warn("titleUpdateServerHandler not registered, dropping {} event from player {}", updateType, playerId);
        }
    }

    @SuppressWarnings("unchecked")
    static void handleRequestSync(ServerPlayer sender, UUID playerId, boolean fullSync) {
        Consumer<RequestSyncContext> h = (Consumer<RequestSyncContext>) handlers.get(HandlerKey.REQUEST_SYNC);
        if (h != null) {
            h.accept(new RequestSyncContext(sender, playerId, fullSync));
        } else {
            LOGGER.warn("requestSyncHandler not registered, dropping sync request from player {}", playerId);
        }
    }

    @SuppressWarnings("unchecked")
    static void handleClusterSync(String sourceServer, String eventType, UUID playerId,
                                  long revision, long timestampMs, String payload) {
        Consumer<ClusterSyncContext> h = (Consumer<ClusterSyncContext>) handlers.get(HandlerKey.CLUSTER_SYNC);
        if (h != null) {
            h.accept(new ClusterSyncContext(sourceServer, eventType, playerId, revision, timestampMs, payload));
        }
    }

    @SuppressWarnings("unchecked")
    static void handleHeadingRequest(ServerPlayer sender, UUID targetUuid, String heading, boolean isGrant) {
        Consumer<HeadingRequestContext> h = (Consumer<HeadingRequestContext>) handlers.get(HandlerKey.HEADING_REQUEST);
        if (h != null) {
            h.accept(new HeadingRequestContext(sender, targetUuid, heading, isGrant));
        }
    }

    public static final class HeadingRequestContext {
        public final ServerPlayer sender;
        public final UUID targetUuid;
        public final String heading;
        public final boolean isGrant;

        public HeadingRequestContext(ServerPlayer sender, UUID targetUuid, String heading, boolean isGrant) {
            this.sender = sender;
            this.targetUuid = targetUuid;
            this.heading = heading;
            this.isGrant = isGrant;
        }
    }

    public static final class SyncPlayerTitlesContext {
        public final UUID playerId;
        public final Set<Integer> unlockedTitleIds;
        public final int equippedTitleId;
        public final Map<String, Integer> killCounts;
        public final int aliveMinutes;
        public final String heading;

        public SyncPlayerTitlesContext(UUID playerId, Set<Integer> unlockedTitleIds,
                                        int equippedTitleId, Map<String, Integer> killCounts,
                                        int aliveMinutes, String heading) {
            this.playerId = playerId;
            this.unlockedTitleIds = unlockedTitleIds;
            this.equippedTitleId = equippedTitleId;
            this.killCounts = killCounts;
            this.aliveMinutes = aliveMinutes;
            this.heading = heading;
        }
    }

    public static final class SyncTitleRegistryContext {
        public final List<TitleDefinition> titles;

        public SyncTitleRegistryContext(List<TitleDefinition> titles) {
            this.titles = titles;
        }
    }

    public static final class TitleUpdateClientContext {
        public final TitleUpdatePacket.UpdateType updateType;
        public final UUID playerId;
        public final int titleId;
        public final String entityId;
        public final int count;

        public TitleUpdateClientContext(TitleUpdatePacket.UpdateType updateType, UUID playerId,
                                         int titleId, String entityId, int count) {
            this.updateType = updateType;
            this.playerId = playerId;
            this.titleId = titleId;
            this.entityId = entityId;
            this.count = count;
        }
    }

    public static final class TitleUpdateServerContext {
        public final ServerPlayer sender;
        public final TitleUpdatePacket.UpdateType updateType;
        public final UUID playerId;
        public final int titleId;

        public TitleUpdateServerContext(ServerPlayer sender, TitleUpdatePacket.UpdateType updateType,
                                         UUID playerId, int titleId) {
            this.sender = sender;
            this.updateType = updateType;
            this.playerId = playerId;
            this.titleId = titleId;
        }
    }

    public static final class RequestSyncContext {
        public final ServerPlayer sender;
        public final UUID playerId;
        public final boolean fullSync;

        public RequestSyncContext(ServerPlayer sender, UUID playerId, boolean fullSync) {
            this.sender = sender;
            this.playerId = playerId;
            this.fullSync = fullSync;
        }
    }

    public static final class ClusterSyncContext {
        public final String sourceServer;
        public final String eventType;
        public final UUID playerId;
        public final long revision;
        public final long timestampMs;
        public final String payload;

        public ClusterSyncContext(String sourceServer, String eventType, UUID playerId,
                                  long revision, long timestampMs, String payload) {
            this.sourceServer = sourceServer;
            this.eventType = eventType;
            this.playerId = playerId;
            this.revision = revision;
            this.timestampMs = timestampMs;
            this.payload = payload;
        }
    }
}
