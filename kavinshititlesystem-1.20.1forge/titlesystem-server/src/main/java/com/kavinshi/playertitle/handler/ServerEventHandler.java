package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.SyncPlayerTitlesPacket;
import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEventHandler {

    private static final Map<java.util.UUID, Integer> aliveTickMap = new ConcurrentHashMap<>();
    private static long lastProgressCheckTick = 0;
    private static final int PROGRESS_CHECK_INTERVAL = 200;

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob mob && mob instanceof Enemy) {
            if (event.getSource().getEntity() instanceof ServerPlayer player) {
                TitleCapability.get(player).ifPresent(state -> {
                    String entityId = mob.getType().getDescriptionId();
                    state.addKill(entityId);
                    RewriteBootstrap.getInstance().getTitleProgressService()
                            .recordKill(state, getTitleRegistry(), entityId, true, mob.getUUID().toString());
                });
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long currentTick = event.getServer().getTickCount();
        if (currentTick - lastProgressCheckTick < PROGRESS_CHECK_INTERVAL) return;
        lastProgressCheckTick = currentTick;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            TitleCapability.get(player).ifPresent(state -> {
                int startTick = aliveTickMap.computeIfAbsent(player.getUUID(), k -> (int) currentTick);
                int aliveMinutes = (int) ((currentTick - startTick) / 1200);
                if (aliveMinutes != state.getAliveMinutes()) {
                    RewriteBootstrap.getInstance().getTitleProgressService()
                            .recordAliveMinutes(state, getTitleRegistry(), aliveMinutes);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            aliveTickMap.put(player.getUUID(), player.server.getTickCount());
            TitleCapability.get(player).ifPresent(state -> {
                syncPlayerData(player, state);
            });
            TitleSyncHandler.syncTitleRegistryToPlayer(player);
            TitleSyncHandler.syncAllEquippedTitlesToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        aliveTickMap.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        TitleCapability.get(event.getOriginal()).ifPresent(oldState -> {
            TitleCapability.get(event.getEntity()).ifPresent(newState -> {
                for (int titleId : oldState.getUnlockedTitleIds()) {
                    newState.unlockTitle(titleId);
                }
                for (var entry : oldState.getKillCounts().entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        newState.addKill(entry.getKey());
                    }
                }
                newState.setEquippedTitleId(oldState.getEquippedTitleId());
                newState.setAliveMinutes(0);
                newState.markClean();
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            aliveTickMap.put(player.getUUID(), player.server.getTickCount());
            TitleCapability.get(player).ifPresent(state -> {
                syncPlayerData(player, state);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TitleCapability.get(player).ifPresent(state -> {
                syncPlayerData(player, state);
            });
        }
    }

    public static void syncPlayerData(ServerPlayer player, PlayerTitleState state) {
        SyncPlayerTitlesPacket packet = SyncPlayerTitlesPacket.fromPlayerTitleState(state);
        NetworkHandler.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
        state.markClean();
    }

    private static TitleRegistry getTitleRegistry() {
        return RewriteBootstrap.getInstance().getTitleRegistry();
    }
}
