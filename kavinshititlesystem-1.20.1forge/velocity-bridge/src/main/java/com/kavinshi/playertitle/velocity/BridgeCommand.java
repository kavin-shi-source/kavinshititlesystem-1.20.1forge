package com.kavinshi.playertitle.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BridgeCommand implements SimpleCommand {
    private final TitleClusterBridgePlugin plugin;

    public BridgeCommand(TitleClusterBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            source.sendMessage(Component.text("Usage: /playertitle-bridge reload").color(NamedTextColor.RED));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.reload();
                source.sendMessage(Component.text("PlayerTitle Bridge configuration and database reloaded successfully!").color(NamedTextColor.GREEN));
            } catch (Exception e) {
                source.sendMessage(Component.text("Failed to reload PlayerTitle Bridge: " + e.getMessage()).color(NamedTextColor.RED));
                plugin.getLogger().error("Failed to reload", e);
            }
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("playertitle.bridge.admin");
    }
}
