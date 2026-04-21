package com.kavinshi.playertitle;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.network.PacketHandlers;
import com.kavinshi.playertitle.network.TitleUpdatePacket;
import com.kavinshi.playertitle.network.CustomTitleUpdatePacket;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.handler.BuffHandler;
import com.kavinshi.playertitle.handler.TitleSyncHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("playertitleserver")
/**
 * 玩家标题系统服务器端主模块，负责初始化服务器端组件和配置。
 * 注册配置、事件处理器和数据包处理器。
 */
public final class KavinshiPlayerTitleMod {
    public static final String MOD_ID = "playertitleserver";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KavinshiPlayerTitleMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TitleConfig.SERVER_SPEC);

        registerServerPacketHandlers();

        RewriteBootstrap.initialize();
        LOGGER.info("Initializing PlayerTitle Server Module");
    }

    private void registerServerPacketHandlers() {
        PacketHandlers.registerTitleUpdateServerHandler(ctx -> {
            if (ctx.updateType != TitleUpdatePacket.UpdateType.TITLE_EQUIPPED) return;
            if (ctx.titleId == -1) {
                BuffHandler.removeBuffs(ctx.sender, ctx.sender.getCapability(TitleCapability.CAPABILITY)
                    .map(s -> s.getEquippedTitleId()).orElse(-1));
                TitleCapability.get(ctx.sender).ifPresent(state -> {
                    state.setEquippedTitleId(-1);
                    TitleSyncHandler.syncPlayerData(ctx.sender);
                });
            } else {
                TitleCapability.get(ctx.sender).ifPresent(state -> {
                    int oldId = state.getEquippedTitleId();
                    com.kavinshi.playertitle.service.EquipResult result =
                        RewriteBootstrap.getInstance().getTitleEquipService()
                            .equip(state, RewriteBootstrap.getInstance().getTitleRegistry(), ctx.titleId);
                    if (result.success()) {
                        if (oldId >= 0) BuffHandler.removeBuffs(ctx.sender, oldId);
                        BuffHandler.applyBuffs(ctx.sender, ctx.titleId);
                        TitleSyncHandler.syncPlayerData(ctx.sender);
                        TitleSyncHandler.broadcastEquippedTitle(ctx.sender);
                    }
                });
            }
        });

        PacketHandlers.registerRequestSyncHandler(ctx -> {
            TitleSyncHandler.syncPlayerData(ctx.sender);
            TitleSyncHandler.syncAllEquippedTitlesToPlayer(ctx.sender);
            TitleSyncHandler.syncTitleRegistryToPlayer(ctx.sender);
        });

        PacketHandlers.registerCustomTitleUpdateHandler(ctx -> {
            TitleCapability.get(ctx.sender).ifPresent(state -> {
                var ct = state.getCustomTitle();
                if (!ct.hasPermission()) {
                    return;
                }

                switch (ctx.updateType) {
                    case SET_TEXT -> {
                        long cooldownMs = TitleConfig.SERVER.customTitleCooldownSeconds.get() * 1000L;
                        if (!ct.canModify(cooldownMs)) {
                            long remaining = ct.getRemainingCooldown(cooldownMs) / 1000L;
                            return;
                        }
                        int maxLen = TitleConfig.SERVER.customTitleMaxLength.get();
                        if (ctx.text.length() > maxLen) return;
                        state.setCustomTitleText(ctx.text);
                    }
                    case SET_COLOR1 -> {
                        long cooldownMs = TitleConfig.SERVER.customTitleCooldownSeconds.get() * 1000L;
                        if (!ct.canModify(cooldownMs)) return;
                        state.setCustomTitleColor1(ctx.color1);
                    }
                    case SET_COLOR2 -> {
                        long cooldownMs = TitleConfig.SERVER.customTitleCooldownSeconds.get() * 1000L;
                        if (!ct.canModify(cooldownMs)) return;
                        if (ct.getPermission() >= com.kavinshi.playertitle.title.CustomTitleData.PERMISSION_GRADIENT) {
                            state.setCustomTitleColor1(ctx.color1);
                            state.setCustomTitleColor2(ctx.color2);
                        }
                    }
                    case TOGGLE_USE -> {
                        LOGGER.info("[TitleSystem] TOGGLE_USE received from {}: useCustom={}, currentText={}",
                            ctx.sender.getGameProfile().getName(), ctx.useCustom, ct.getText());
                        if (ctx.useCustom && ct.getText().isEmpty()) {
                            LOGGER.info("[TitleSystem] Cannot enable custom title: text is empty");
                            return;
                        }
                        state.setUsingCustomTitle(ctx.useCustom);
                        if (ctx.useCustom) {
                            int oldId = state.getEquippedTitleId();
                            if (oldId >= 0) {
                                BuffHandler.removeBuffs(ctx.sender, oldId);
                                state.setEquippedTitleId(-1);
                            }
                        }
                    }
                }
                TitleSyncHandler.syncPlayerData(ctx.sender);
                if (ctx.updateType == CustomTitleUpdatePacket.UpdateType.TOGGLE_USE) {
                    TitleSyncHandler.broadcastEquippedTitle(ctx.sender);
                }
            });
        });
    }
}
