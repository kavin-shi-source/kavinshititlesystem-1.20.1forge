package com.kavinshi.playertitle.network;

import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.title.TitleDefinition;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class PacketHandlers {
    private PacketHandlers() {}

    private static Consumer<SyncPlayerTitlesContext> syncPlayerTitlesHandler;
    private static Consumer<SyncTitleRegistryContext> syncTitleRegistryHandler;
    private static Consumer<TitleUpdateClientContext> titleUpdateClientHandler;
    private static Consumer<TitleUpdateServerContext> titleUpdateServerHandler;
    private static Consumer<RequestSyncContext> requestSyncHandler;
    private static Consumer<CustomTitleUpdateContext> customTitleUpdateHandler;
    private static Consumer<ClusterSyncContext> clusterSyncHandler;

    public static void registerClusterSyncHandler(Consumer<ClusterSyncContext> handler) {
        clusterSyncHandler = handler;
    }

    public static void registerSyncPlayerTitlesHandler(Consumer<SyncPlayerTitlesContext> handler) {
        syncPlayerTitlesHandler = handler;
    }

    public static void registerSyncTitleRegistryHandler(Consumer<SyncTitleRegistryContext> handler) {
        syncTitleRegistryHandler = handler;
    }

    public static void registerTitleUpdateClientHandler(Consumer<TitleUpdateClientContext> handler) {
        titleUpdateClientHandler = handler;
    }

    public static void registerTitleUpdateServerHandler(Consumer<TitleUpdateServerContext> handler) {
        titleUpdateServerHandler = handler;
    }

    public static void registerRequestSyncHandler(Consumer<RequestSyncContext> handler) {
        requestSyncHandler = handler;
    }

    public static void registerCustomTitleUpdateHandler(Consumer<CustomTitleUpdateContext> handler) {
        customTitleUpdateHandler = handler;
    }

    static void handleSyncPlayerTitles(UUID playerId, Set<Integer> unlockedTitleIds,
                                        int equippedTitleId, Map<String, Integer> killCounts,
                                        int aliveMinutes, CustomTitleData customTitle) {
        if (syncPlayerTitlesHandler != null) {
            syncPlayerTitlesHandler.accept(new SyncPlayerTitlesContext(
                playerId, unlockedTitleIds, equippedTitleId, killCounts, aliveMinutes, customTitle));
        }
    }

    static void handleSyncTitleRegistry(List<TitleDefinition> titles) {
        if (syncTitleRegistryHandler != null) {
            syncTitleRegistryHandler.accept(new SyncTitleRegistryContext(titles));
        }
    }

    static void handleTitleUpdateClient(TitleUpdatePacket.UpdateType updateType, UUID playerId,
                                         int titleId, String entityId, int count) {
        if (titleUpdateClientHandler != null) {
            titleUpdateClientHandler.accept(new TitleUpdateClientContext(updateType, playerId, titleId, entityId, count));
        }
    }

    static void handleTitleUpdateServer(ServerPlayer sender, TitleUpdatePacket.UpdateType updateType,
                                         UUID playerId, int titleId) {
        if (titleUpdateServerHandler != null) {
            titleUpdateServerHandler.accept(new TitleUpdateServerContext(sender, updateType, playerId, titleId));
        }
    }

    static void handleRequestSync(ServerPlayer sender, UUID playerId, boolean fullSync) {
        if (requestSyncHandler != null) {
            requestSyncHandler.accept(new RequestSyncContext(sender, playerId, fullSync));
        }
    }

    static void handleCustomTitleUpdateServer(ServerPlayer sender, CustomTitleUpdatePacket.UpdateType updateType,
                                              String text, int color1, int color2, boolean useCustom) {
        if (customTitleUpdateHandler != null) {
            customTitleUpdateHandler.accept(new CustomTitleUpdateContext(sender, updateType, text, color1, color2, useCustom));
        }
    }

    public static final class SyncPlayerTitlesContext {
        public final UUID playerId;
        public final Set<Integer> unlockedTitleIds;
        public final int equippedTitleId;
        public final Map<String, Integer> killCounts;
        public final int aliveMinutes;
        public final CustomTitleData customTitle;

        public SyncPlayerTitlesContext(UUID playerId, Set<Integer> unlockedTitleIds,
                                        int equippedTitleId, Map<String, Integer> killCounts,
                                        int aliveMinutes, CustomTitleData customTitle) {
            this.playerId = playerId;
            this.unlockedTitleIds = unlockedTitleIds;
            this.equippedTitleId = equippedTitleId;
            this.killCounts = killCounts;
            this.aliveMinutes = aliveMinutes;
            this.customTitle = customTitle;
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

    static void handleClusterSync(String sourceServer, String eventType, UUID playerId,
                                  long revision, long timestampMs, String payload) {
        if (clusterSyncHandler != null) {
            clusterSyncHandler.accept(new ClusterSyncContext(sourceServer, eventType, playerId, revision, timestampMs, payload));
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

    public static final class CustomTitleUpdateContext {
        public final ServerPlayer sender;
        public final CustomTitleUpdatePacket.UpdateType updateType;
        public final String text;
        public final int color1;
        public final int color2;
        public final boolean useCustom;

        public CustomTitleUpdateContext(ServerPlayer sender, CustomTitleUpdatePacket.UpdateType updateType,
                                        String text, int color1, int color2, boolean useCustom) {
            this.sender = sender;
            this.updateType = updateType;
            this.text = text;
            this.color1 = color1;
            this.color2 = color2;
            this.useCustom = useCustom;
        }
    }
}
