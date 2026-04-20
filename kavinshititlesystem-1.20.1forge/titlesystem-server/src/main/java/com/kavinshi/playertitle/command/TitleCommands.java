package com.kavinshi.playertitle.command;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.JsonTitleConfigRepository;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.handler.BuffHandler;
import com.kavinshi.playertitle.handler.TitleSyncHandler;
import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.service.EquipResult;
import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
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
                    .then(Commands.argument("titleId", IntegerArgumentType.integer(-1))
                        .executes(TitleCommands::equip)))
                .then(Commands.literal("unequip")
                    .executes(TitleCommands::unequip))
                .then(Commands.literal("custom")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("solid")
                            .executes(ctx -> setCustomPermission(ctx, CustomTitleData.PERMISSION_SOLID)))
                        .then(Commands.literal("gradient")
                            .executes(ctx -> setCustomPermission(ctx, CustomTitleData.PERMISSION_GRADIENT)))
                        .then(Commands.literal("rainbow")
                            .executes(ctx -> setCustomPermission(ctx, CustomTitleData.PERMISSION_RAINBOW)))
                        .then(Commands.literal("off")
                            .executes(ctx -> setCustomPermission(ctx, CustomTitleData.PERMISSION_NONE)))))
                .then(Commands.literal("setcustom")
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(TitleCommands::setCustomText)))
                .then(Commands.literal("setcolor")
                    .then(Commands.argument("color1", IntegerArgumentType.integer(0, 0xFFFFFF))
                        .executes(TitleCommands::setColor1)
                        .then(Commands.argument("color2", IntegerArgumentType.integer(0, 0xFFFFFF))
                            .executes(TitleCommands::setColor2))))
                .then(Commands.literal("usecustom")
                    .then(Commands.argument("enable", StringArgumentType.word())
                        .executes(TitleCommands::toggleCustom)))
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
            }
            state.revokeTitle(titleId);
            TitleSyncHandler.syncPlayerData(target);
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
            CustomTitleData ct = state.getCustomTitle();
            sb.append(" | Custom: ").append(ct.getPermissionName());
            if (ct.hasPermission()) {
                sb.append(" Text=\"").append(ct.getText()).append("\"");
                sb.append(" Using=").append(ct.isUsingCustomTitle());
                sb.append(" C1=#").append(Integer.toHexString(ct.getColor1()));
                if (ct.getPermission() >= CustomTitleData.PERMISSION_GRADIENT) {
                    sb.append(" C2=#").append(Integer.toHexString(ct.getColor2()));
                }
            }
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
                state.setUsingCustomTitle(false);
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

    private static int setCustomPermission(CommandContext<CommandSourceStack> ctx, int permission) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        return TitleCapability.get(target).map(state -> {
            state.setCustomTitlePermission(permission);
            TitleSyncHandler.syncPlayerData(target);
            String permName = switch (permission) {
                case CustomTitleData.PERMISSION_SOLID -> "Solid (single color)";
                case CustomTitleData.PERMISSION_GRADIENT -> "Gradient (two-color blend)";
                case CustomTitleData.PERMISSION_RAINBOW -> "Rainbow (RGB marquee)";
                default -> "None (disabled)";
            };
            ctx.getSource().sendSuccess(() -> Component.literal("Set custom title permission for " + target.getName().getString() + " to: " + permName), true);
            return 1;
        }).orElse(0);
    }

    private static int setCustomText(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String text = StringArgumentType.getString(ctx, "text");
        int maxLen = TitleConfig.SERVER.customTitleMaxLength.get();

        return TitleCapability.get(player).map(state -> {
            CustomTitleData ct = state.getCustomTitle();
            if (!ct.hasPermission()) {
                ctx.getSource().sendFailure(Component.literal("You do not have custom title permission"));
                return 0;
            }
            if (text.length() > maxLen) {
                ctx.getSource().sendFailure(Component.literal("Text too long (max " + maxLen + " characters)"));
                return 0;
            }
            long cooldownMs = TitleConfig.SERVER.customTitleCooldownSeconds.get() * 1000L;
            if (!ct.canModify(cooldownMs)) {
                long remaining = ct.getRemainingCooldown(cooldownMs) / 1000L;
                ctx.getSource().sendFailure(Component.literal("Cooldown: " + remaining + " seconds remaining"));
                return 0;
            }
            state.setCustomTitleText(text);
            TitleSyncHandler.syncPlayerData(player);
            TitleSyncHandler.broadcastEquippedTitle(player);
            MutableComponent msg = Component.literal("Custom title set to: ");
            MutableComponent titleComp = Component.literal("[" + text + "]")
                .withStyle(s -> s.withColor(TextColor.fromRgb(ct.getColor1() & 0xFFFFFF)));
            msg.append(titleComp);
            ctx.getSource().sendSuccess(() -> msg, false);
            return 1;
        }).orElse(0);
    }

    private static int setColor1(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int color1 = IntegerArgumentType.getInteger(ctx, "color1");

        return TitleCapability.get(player).map(state -> {
            CustomTitleData ct = state.getCustomTitle();
            if (!ct.hasPermission()) {
                ctx.getSource().sendFailure(Component.literal("You do not have custom title permission"));
                return 0;
            }
            long cooldownMs = TitleConfig.SERVER.customTitleCooldownSeconds.get() * 1000L;
            if (!ct.canModify(cooldownMs)) {
                long remaining = ct.getRemainingCooldown(cooldownMs) / 1000L;
                ctx.getSource().sendFailure(Component.literal("Cooldown: " + remaining + " seconds remaining"));
                return 0;
            }
            state.setCustomTitleColor1(color1);
            TitleSyncHandler.syncPlayerData(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Color 1 set to #" + Integer.toHexString(color1)), false);
            return 1;
        }).orElse(0);
    }

    private static int setColor2(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int color1 = IntegerArgumentType.getInteger(ctx, "color1");
        int color2 = IntegerArgumentType.getInteger(ctx, "color2");

        return TitleCapability.get(player).map(state -> {
            CustomTitleData ct = state.getCustomTitle();
            if (ct.getPermission() < CustomTitleData.PERMISSION_GRADIENT) {
                ctx.getSource().sendFailure(Component.literal("Gradient permission required for second color"));
                return 0;
            }
            long cooldownMs = TitleConfig.SERVER.customTitleCooldownSeconds.get() * 1000L;
            if (!ct.canModify(cooldownMs)) {
                long remaining = ct.getRemainingCooldown(cooldownMs) / 1000L;
                ctx.getSource().sendFailure(Component.literal("Cooldown: " + remaining + " seconds remaining"));
                return 0;
            }
            state.setCustomTitleColor1(color1);
            state.setCustomTitleColor2(color2);
            TitleSyncHandler.syncPlayerData(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Colors set to #" + Integer.toHexString(color1) + " -> #" + Integer.toHexString(color2)), false);
            return 1;
        }).orElse(0);
    }

    private static int toggleCustom(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String enable = StringArgumentType.getString(ctx, "enable");

        return TitleCapability.get(player).map(state -> {
            CustomTitleData ct = state.getCustomTitle();
            if (!ct.hasPermission()) {
                ctx.getSource().sendFailure(Component.literal("You do not have custom title permission"));
                return 0;
            }
            boolean use = enable.equalsIgnoreCase("on") || enable.equalsIgnoreCase("true") || enable.equals("1");
            if (use && ct.getText().isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Set custom title text first with /playertitle setcustom <text>"));
                return 0;
            }
            if (use) {
                int oldId = state.getEquippedTitleId();
                if (oldId >= 0) {
                    BuffHandler.removeBuffs(player, oldId);
                    state.setEquippedTitleId(-1);
                }
            }
            state.setUsingCustomTitle(use);
            TitleSyncHandler.syncPlayerData(player);
            TitleSyncHandler.broadcastEquippedTitle(player);
            ctx.getSource().sendSuccess(() -> Component.literal(use ? "Using custom title" : "Disabled custom title"), false);
            return 1;
        }).orElse(0);
    }
}
