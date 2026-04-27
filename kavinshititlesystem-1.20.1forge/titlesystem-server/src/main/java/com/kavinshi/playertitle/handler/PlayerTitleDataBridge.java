package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.ModConstants;
import com.kavinshi.playertitle.network.TitleDataSyncPacket;
import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.sync.ClusterConfig;
import com.kavinshi.playertitle.sync.ClusterEventBus;
import com.kavinshi.playertitle.sync.ClusterEventType;
import com.kavinshi.playertitle.sync.ClusterSyncEvent;
import com.kavinshi.playertitle.sync.TitleAssignedEvent;
import com.kavinshi.playertitle.sync.TitleEquipStateChangedEvent;
import com.kavinshi.playertitle.sync.TitleProgressUpdatedEvent;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PlayerTitleDataBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerTitleDataBridge.class);
    private static final ResourceLocation DATA_CHANNEL = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "data");

    private final MinecraftServer server;
    private final TitleRegistry titleRegistry;
    private final ClusterConfig clusterConfig;

    public PlayerTitleDataBridge(MinecraftServer server, TitleRegistry titleRegistry, ClusterConfig clusterConfig) {
        this.server = Objects.requireNonNull(server, "server");
        this.titleRegistry = Objects.requireNonNull(titleRegistry, "titleRegistry");
        this.clusterConfig = Objects.requireNonNull(clusterConfig, "clusterConfig");
    }

    public void subscribeToEvents(ClusterEventBus eventBus) {
        try {
            eventBus.subscribe(ClusterEventType.TITLE_EQUIP_STATE_CHANGED, this::onTitleEquipChanged);
            eventBus.subscribe(ClusterEventType.TITLE_ASSIGNED, this::onTitleAssigned);
            eventBus.subscribe(ClusterEventType.TITLE_REMOVED, this::onTitleRemoved);
            eventBus.subscribe(ClusterEventType.TITLE_PROGRESS_UPDATED, this::onTitleProgressUpdated);
            LOGGER.info("PlayerTitleDataBridge subscribed to cluster events");
        } catch (ClusterEventBus.EventBusException e) {
            LOGGER.error("Failed to subscribe to cluster events", e);
        }
    }

    private void onTitleEquipChanged(ClusterSyncEvent event) {
        if (!(event instanceof TitleEquipStateChangedEvent equipEvent)) return;
        triggerSync(equipEvent.getPlayerId());
    }

    private void onTitleAssigned(ClusterSyncEvent event) {
        if (!(event instanceof TitleAssignedEvent assignedEvent)) return;
        triggerSync(assignedEvent.getPlayerId());
    }

    private void onTitleRemoved(ClusterSyncEvent event) {
        triggerSync(event.getPlayerId());
    }

    private void onTitleProgressUpdated(ClusterSyncEvent event) {
        if (!(event instanceof TitleProgressUpdatedEvent progressEvent)) return;
        if (progressEvent.isCompleted()) {
            triggerSync(progressEvent.getPlayerId());
        }
    }

    public void triggerSync(UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return;

        ServerPlayer carrier = findAnyOnlinePlayer();
        if (carrier == null) return;

        String playerName = player.getGameProfile().getName();
        int titleId = -1;
        String titleName = "";
        int titleColor = 0xFFFFFF;
        String heading = "";

        PlayerTitleState state = TitleCapability.get(player).orElse(null);
        if (state != null) {
            heading = state.getHeading();
            int equippedId = state.getEquippedTitleId();
            if (equippedId >= 0) {
                titleId = equippedId;
                TitleDefinition def = titleRegistry.getTitle(equippedId);
                if (def != null) {
                    titleName = def.getName();
                    titleColor = def.getColor();
                }
            }
        }

        TitleDataSyncPacket packet = new TitleDataSyncPacket(
            playerId, playerName, clusterConfig.getServerName(), titleId, titleName, titleColor, heading
        );

        sendToProxy(carrier, packet);
    }

    private void sendToProxy(ServerPlayer carrier, TitleDataSyncPacket packet) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBytes(packet.toBytes());

            ClientboundCustomPayloadPacket payload = new ClientboundCustomPayloadPacket(
                DATA_CHANNEL,
                buf
            );
            carrier.connection.send(payload);
        } catch (Exception e) {
            LOGGER.error("Failed to send title data to proxy via player {}", carrier.getGameProfile().getName(), e);
        }
    }

    private ServerPlayer findAnyOnlinePlayer() {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }
}
