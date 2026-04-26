package com.kavinshi.playertitle.command;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.JsonTitleConfigRepository;
import com.kavinshi.playertitle.handler.BuffHandler;
import com.kavinshi.playertitle.handler.TitleSyncHandler;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.service.EquipResult;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

import java.nio.file.Path;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("null")
public final class TitleCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("playertitle")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(TitleCommands::reload))
                .then(Commands.literal("grant")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("titleId", IntegerArgumentType.integer(1))
                            .executes(TitleCommands::grant))))
                .then(Commands.literal("revoke")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("titleId", IntegerArgumentType.integer(1))
                            .executes(TitleCommands::revoke))))
                .then(Commands.literal("check")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(TitleCommands::check)))
                .then(Commands.literal("equip")
                    .then(Commands.argument("titleId", IntegerArgumentType.integer(0))
                        .executes(TitleCommands::equip)))
                .then(Commands.literal("unequip")
                    .executes(TitleCommands::unequip))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        try {
            Path configPath = Path.of("config", "playertitle", "titles.json");
            var definitions = new JsonTitleConfigRepository().loadDefinitions(configPath);
            RewriteBootstrap.getInstance().getTitleRegistry().loadAll(definitions);
            src.sendSuccess(() -> Component.literal("Reloaded " + definitions.size() + " title definitions"), true);
            return definitions.size();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Failed to reload: " + e.getMessage()));
            return 0;
        }
    }

    private static int grant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int titleId = IntegerArgumentType.getInteger(ctx, "titleId");
        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();

        if (registry.getTitle(titleId) == null) {
            ctx.getSource().sendFailure(Component.literal("Title not found: " + titleId));
            return 0;
        }

        return TitleCapability.get(target).map(state -> {
            if (state.isTitleUnlocked(titleId)) {
                ctx.getSource().sendFailure(Component.literal("Player already has title: " + titleId));
                return 0;
            }
            state.unlockTitle(titleId);
            TitleSyncHandler.syncPlayerData(target);
            TitleDefinition def = registry.getTitle(titleId);
            ctx.getSource().sendSuccess(() -> Component.literal("Granted title [" + titleId + "] " + (def != null ? def.getName() : "") + " to " + target.getName().getString()), true);
            return 1;
        }).orElse(0);
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int titleId = IntegerArgumentType.getInteger(ctx, "titleId");

        return TitleCapability.get(target).map(state -> {
            if (!state.isTitleUnlocked(titleId)) {
                ctx.getSource().sendFailure(Component.literal("Player does not have title: " + titleId));
                return 0;
            }
            if (state.getEquippedTitleId() == titleId) {
                BuffHandler.removeBuffs(target, titleId);
                state.setEquippedTitleId(-1);
            }
            state.revokeTitle(titleId);
            TitleSyncHandler.syncPlayerData(target);
            TitleSyncHandler.broadcastEquippedTitle(target);
            ctx.getSource().sendSuccess(() -> Component.literal("Revoked title [" + titleId + "] from " + target.getName().getString()), true);
            return 1;
        }).orElse(0);
    }

    private static int check(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        return TitleCapability.get(target).map(state -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getName().getString());
            sb.append(" | Equipped: ").append(state.getEquippedTitleId());
            sb.append(" | Unlocked: ").append(state.getUnlockedTitleIds());
            sb.append(" | Alive: ").append(state.getAliveMinutes()).append("min");
            ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            return 1;
        }).orElse(0);
    }

    private static int equip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int titleId = IntegerArgumentType.getInteger(ctx, "titleId");

        return TitleCapability.get(player).map(state -> {
            int oldTitleId = state.getEquippedTitleId();
            EquipResult result = RewriteBootstrap.getInstance().getTitleEquipService()
                    .equip(state, RewriteBootstrap.getInstance().getTitleRegistry(), titleId);
            if (result.success()) {
                if (oldTitleId >= 0) BuffHandler.removeBuffs(player, oldTitleId);
                BuffHandler.applyBuffs(player, titleId);
                TitleSyncHandler.syncPlayerData(player);
                TitleSyncHandler.broadcastEquippedTitle(player);
                ctx.getSource().sendSuccess(() -> Component.literal("Equipped title: " + titleId), false);
            } else {
                ctx.getSource().sendFailure(Component.literal("Cannot equip: " + result.reason()));
            }
            return result.success() ? 1 : 0;
        }).orElse(0);
    }

    private static int unequip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        return TitleCapability.get(player).map(state -> {
            int oldTitleId = state.getEquippedTitleId();
            EquipResult result = RewriteBootstrap.getInstance().getTitleEquipService().unequip(state);
            if (result.success()) {
                if (oldTitleId >= 0) BuffHandler.removeBuffs(player, oldTitleId);
                TitleSyncHandler.syncPlayerData(player);
                ctx.getSource().sendSuccess(() -> Component.literal("Unequipped title"), false);
            } else {
                ctx.getSource().sendFailure(Component.literal("Cannot unequip: " + result.reason()));
            }
            return result.success() ? 1 : 0;
        }).orElse(0);
    }

}
