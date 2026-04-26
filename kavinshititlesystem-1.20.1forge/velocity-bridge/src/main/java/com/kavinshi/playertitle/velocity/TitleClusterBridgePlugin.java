package com.kavinshi.playertitle.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.kavinshi.playertitle.velocity.api.RestServer;
import com.kavinshi.playertitle.velocity.database.DatabaseManager;
import com.kavinshi.playertitle.velocity.service.HeadingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
    id = "playertitle-cluster-bridge",
    name = "PlayerTitle Cluster Bridge",
    version = "1.0.0",
    description = "Forwards PlayerTitle cluster sync events and provides title data via MiniPlaceholders"
)
public class TitleClusterBridgePlugin {

    public static final String CLUSTER_CHANNEL = "playertitle:main";
    public static final String DATA_CHANNEL = "playertitle:data";
    public static final String CHAT_CHANNEL = "playertitle:chat";
    private static final String CHANNEL_NAMESPACE = "playertitle";

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final TitleCache titleCache;
    private final TitlePlaceholdersExpansion placeholders;
    private volatile BridgeConfig config = new BridgeConfig(0xAAAAAA, true, "localhost", 3306, "playertitle", "root", "", 5000L, true);

    private MinecraftChannelIdentifier clusterId;
    private MinecraftChannelIdentifier dataId;
    private MinecraftChannelIdentifier chatId;

    @Inject
    public TitleClusterBridgePlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.titleCache = new TitleCache();
        this.placeholders = new TitlePlaceholdersExpansion(titleCache);
    }

    private DatabaseManager dbManager;
    private HeadingService headingService;
    private RestServer restServer;

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        this.config = BridgeConfig.load(dataDirectory, logger);

        clusterId = MinecraftChannelIdentifier.from(CLUSTER_CHANNEL);
        dataId = MinecraftChannelIdentifier.from(DATA_CHANNEL);
        chatId = MinecraftChannelIdentifier.from(CHAT_CHANNEL);

        proxy.getChannelRegistrar().register(clusterId, dataId, chatId);

        // 注册 MiniPlaceholders 扩展
        if (proxy.getPluginManager().getPlugin("miniplaceholders").isPresent()) {
            placeholders.register();
            logger.info("MiniPlaceholders expansion registered.");
        } else {
            logger.warn("MiniPlaceholders not found! Placeholders will not be registered.");
        }

        // 初始化数据库、服务和 API
        try {
            dbManager = new DatabaseManager(config, dataDirectory);
            headingService = new HeadingService(dbManager, titleCache);
            restServer = new RestServer(8080, "test-secret-key-replace-in-prod", headingService);
            logger.info("Database and REST API initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize database and REST API", e);
        }

        proxy.getScheduler().buildTask(this, () -> {
            int evicted = titleCache.evictStaleEntries();
            if (evicted > 0) {
                logger.debug("Evicted {} stale title cache entries", evicted);
            }
        }).repeat(5, TimeUnit.MINUTES).schedule();

        logger.info("Registered channels: {}, {}, {}", CLUSTER_CHANNEL, DATA_CHANNEL, CHAT_CHANNEL);
        logger.info("PlayerTitle Cluster Bridge initialized");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        String channelId = event.getIdentifier().getId();

        if (CLUSTER_CHANNEL.equals(channelId)) {
            handleClusterMessage(event);
        } else if (DATA_CHANNEL.equals(channelId)) {
            handleDataMessage(event);
        } else if (CHAT_CHANNEL.equals(channelId)) {
            handleChatMessage(event);
        }
    }

    private static final int MAX_CLUSTER_MESSAGE_SIZE = 65536;

    private void handleClusterMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection sourceConn)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        String sourceServerName = sourceConn.getServerInfo().getName();
        byte[] data = event.getData();

        if (data.length > MAX_CLUSTER_MESSAGE_SIZE) {
            logger.warn("Cluster message from {} too large ({} bytes), dropping", sourceServerName, data.length);
            return;
        }

        logger.debug("Received cluster sync message from server: {} ({} bytes)", sourceServerName, data.length);

        for (com.velocitypowered.api.proxy.server.RegisteredServer server : proxy.getAllServers()) {
            if (server.getServerInfo().getName().equals(sourceServerName)) {
                continue;
            }

            if (server.getPlayersConnected().isEmpty()) {
                continue;
            }

            server.sendPluginMessage(clusterId, data);
        }
    }

    private void handleDataMessage(PluginMessageEvent event) {
        byte[] data = event.getData();
        try {
            TitleDataSyncPacket packet = TitleDataSyncPacket.fromBytes(data);
            event.setResult(PluginMessageEvent.ForwardResult.handled());
            UUID playerId = packet.getPlayerId();

            TitleCache.TitleEntry entry = new TitleCache.TitleEntry(
                playerId, packet.getPlayerName(), packet.getServerName(), packet.getEquippedTitleId(),
                packet.getTitleName(), packet.getTitleColor(), packet.getHeading()
            );

            titleCache.update(entry);

            logger.debug("Updated title cache for {} (UUID: {}) on server {}",
                packet.getPlayerName(), playerId, packet.getServerName());
        } catch (Exception e) {
            logger.error("Failed to process title data message ({} bytes)", data.length, e);
        }
    }

    private void handleChatMessage(PluginMessageEvent event) {
        byte[] data = event.getData();
        try {
            ChatMessagePacket packet = ChatMessagePacket.fromBytes(data);
            event.setResult(PluginMessageEvent.ForwardResult.handled());
            UUID playerId = packet.getPlayerId();
            String playerName = packet.getPlayerName();
            String serverName = packet.getServerName();
            String rawMessage = packet.getMessage();

            Optional<Player> proxyPlayer = proxy.getPlayer(playerId);
            if (proxyPlayer.isEmpty()) {
                logger.debug("Ignoring chat from disconnected player: {}", playerName);
                return;
            }

            Component chatComponent = buildChatComponent(playerId, playerName, serverName, rawMessage);
            for (Player onlinePlayer : proxy.getAllPlayers()) {
                if (!onlinePlayer.getUniqueId().equals(playerId)) {
                    onlinePlayer.sendMessage(chatComponent);
                }
            }

            logger.debug("Broadcast cross-server chat from {} on {}: {}", playerName, serverName, rawMessage);
        } catch (Exception e) {
            logger.error("Failed to process chat message ({} bytes)", data.length, e);
        }
    }

    private Component buildChatComponent(UUID playerId, String playerName, String serverName, String rawMessage) {
        Optional<TitleCache.TitleEntry> entry = titleCache.get(playerId);

        Component headingComponent = Component.empty();
        Component titleComponent = Component.empty();
        Component nameComponent = Component.text(playerName);
        Component serverComponent = Component.text("[" + serverName + "]", TextColor.color(config.getServerNameColor()));
        Component messageComponent = Component.text(" : " + rawMessage);

        if (entry.isPresent()) {
            TitleCache.TitleEntry te = entry.get();
            String heading = te.getHeading();
            if (!heading.isEmpty()) {
                headingComponent = Component.text("[" + heading + "]");
            }

            if (te.isHasTitle()) {
                String effectiveTitle = te.getEffectiveTitle();
                int effectiveColor = te.getEffectiveColor();

                if (!effectiveTitle.isEmpty()) {
                    if (effectiveColor >= 0) {
                        titleComponent = Component.text("[" + effectiveTitle + "]", TextColor.color(effectiveColor));
                    } else {
                        titleComponent = Component.text("[" + effectiveTitle + "]");
                    }
                }
            }
        }

        Component result = Component.text().append(serverComponent).build();
        if (headingComponent != Component.empty()) {
            result = result.append(headingComponent);
        }
        if (titleComponent != Component.empty()) {
            result = result.append(titleComponent);
        }
        return result.append(nameComponent).append(messageComponent);
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        logger.debug("Player joined: {} ({}), title data will arrive from backend server",
            player.getUsername(), player.getUniqueId());
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        titleCache.remove(playerId);
        logger.debug("Removed title cache entry for disconnected player: {}",
            event.getPlayer().getUsername());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        logger.debug("Player {} switched servers, will update title data when new sync arrives",
            player.getUsername());
    }

    public TitleCache getTitleCache() {
        return titleCache;
    }

    public TitlePlaceholdersExpansion getPlaceholders() {
        return placeholders;
    }
}
