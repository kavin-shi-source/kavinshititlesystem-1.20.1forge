package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.SyncPlayerTitlesPacket;
import com.kavinshi.playertitle.network.SyncTitleRegistryPacket;
import com.kavinshi.playertitle.network.TitleUpdatePacket;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class TitleSyncHandler {

    public static void syncPlayerData(ServerPlayer player) {
        TitleCapability.get(player).ifPresent(state -> {
            SyncPlayerTitlesPacket packet = SyncPlayerTitlesPacket.fromPlayerTitleState(state);
            NetworkHandler.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
            state.markClean();
        });
    }

    public static void broadcastEquippedTitle(ServerPlayer player) {
        TitleCapability.get(player).ifPresent(state -> {
            CustomTitleData ct = state.getCustomTitle();
            if (ct.isUsingCustomTitle() && ct.hasPermission()) {
                return;
            }

            int titleId = state.getEquippedTitleId();
            if (titleId < 0) return;

            TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
            TitleDefinition title = registry.getTitle(titleId);
            if (title == null) return;

            TitleUpdatePacket packet = new TitleUpdatePacket(
                    player.getUUID(), TitleUpdatePacket.UpdateType.TITLE_EQUIPPED, titleId);
            NetworkHandler.getChannel().send(PacketDistributor.ALL.noArg(), packet);
        });
    }

    public static void syncAllEquippedTitlesToPlayer(ServerPlayer newPlayer) {
        var server = newPlayer.server;
        if (server == null) return;

        for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
            if (otherPlayer == newPlayer) continue;
            TitleCapability.get(otherPlayer).ifPresent(state -> {
                CustomTitleData ct = state.getCustomTitle();
                if (ct.isUsingCustomTitle() && ct.hasPermission()) {
                    return;
                }

                int titleId = state.getEquippedTitleId();
                if (titleId < 0) return;

                TitleUpdatePacket packet = new TitleUpdatePacket(
                        otherPlayer.getUUID(), TitleUpdatePacket.UpdateType.TITLE_EQUIPPED, titleId);
                NetworkHandler.getChannel().send(PacketDistributor.PLAYER.with(() -> newPlayer), packet);
            });
        }
    }

    public static void syncTitleRegistryToPlayer(ServerPlayer player) {
        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
        SyncTitleRegistryPacket packet = new SyncTitleRegistryPacket(registry.getAllTitlesSorted());
        NetworkHandler.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
