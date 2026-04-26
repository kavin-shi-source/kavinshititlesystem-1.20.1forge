package com.kavinshi.playertitle.command;

import com.kavinshi.playertitle.network.HeadingRequestPacket;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("null")
public final class TitleAdminCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("titleadmin")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("grant")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("heading", StringArgumentType.greedyString())
                            .executes(ctx -> grantHeading(ctx)))))
                .then(Commands.literal("revoke")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> revokeHeading(ctx))))
        );
    }

    private static int grantHeading(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String heading = StringArgumentType.getString(ctx, "heading");
        
        HeadingRequestPacket packet = new HeadingRequestPacket(target.getUUID(), heading, true);
        NetworkHandler.getChannel().sendToServer(packet);
        // Fallback for local changes just in case:
        com.kavinshi.playertitle.player.TitleCapability.get(target).ifPresent(state -> {
            state.setHeading(heading);
            com.kavinshi.playertitle.handler.TitleSyncHandler.syncPlayerData(target);
            com.kavinshi.playertitle.handler.TitleSyncHandler.broadcastEquippedTitle(target);
        });
        ctx.getSource().sendSuccess(() -> Component.literal("Sent grant heading request for " + target.getName().getString()), true);
        return 1;
    }

    private static int revokeHeading(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        
        HeadingRequestPacket packet = new HeadingRequestPacket(target.getUUID(), "", false);
        NetworkHandler.getChannel().sendToServer(packet);
        // Fallback for local changes just in case:
        com.kavinshi.playertitle.player.TitleCapability.get(target).ifPresent(state -> {
            state.setHeading("");
            com.kavinshi.playertitle.handler.TitleSyncHandler.syncPlayerData(target);
            com.kavinshi.playertitle.handler.TitleSyncHandler.broadcastEquippedTitle(target);
        });
        ctx.getSource().sendSuccess(() -> Component.literal("Sent revoke heading request for " + target.getName().getString()), true);
        return 1;
    }
}