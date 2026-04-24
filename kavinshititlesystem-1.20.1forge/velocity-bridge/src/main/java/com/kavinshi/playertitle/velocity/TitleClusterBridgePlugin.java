package com.kavinshi.playertitle.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

@Plugin(
    id = "playertitle-cluster-bridge",
    name = "PlayerTitle Cluster Bridge",
    version = "1.0.0",
    description = "Forwards PlayerTitle cluster sync events between backend Forge servers"
)
public class TitleClusterBridgePlugin {

    public static final String CHANNEL_ID = "playertitle:main";

    private final ProxyServer proxy;
    private final Logger logger;

    @Inject
    public TitleClusterBridgePlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        proxy.getChannelRegistrar().register(
            com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(CHANNEL_ID)
        );
        logger.info("PlayerTitle Cluster Bridge initialized, listening on channel: {}", CHANNEL_ID);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals(CHANNEL_ID)) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection sourceConn)) {
            return;
        }

        String sourceServerName = sourceConn.getServerInfo().getName();
        byte[] data = event.getData();

        logger.debug("Received cluster sync message from server: {} ({} bytes)", sourceServerName, data.length);

        for (RegisteredServer server : proxy.getAllServers()) {
            if (server.getServerInfo().getName().equals(sourceServerName)) {
                continue;
            }

            if (server.getPlayersConnected().isEmpty()) {
                continue;
            }

            server.sendPluginMessage(
                com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(CHANNEL_ID),
                data
            );

            logger.debug("Forwarded cluster sync message to server: {}", server.getServerInfo().getName());
        }
    }
}
