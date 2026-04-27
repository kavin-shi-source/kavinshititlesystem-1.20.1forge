package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.SyncPlayerTitlesPacket;
import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.nbt.CompoundTag;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEventHandler.class);

    private static final Map<UUID, Long> aliveTickMap = new ConcurrentHashMap<>();
    private static long lastProgressCheckTick = 0;
    private static int currentPlayerIndex = 0;

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob mob && mob instanceof Enemy) {
            if (event.getSource().getEntity() instanceof ServerPlayer player) {
                TitleCapability.get(player).ifPresent(state -> {
                    String entityId = mob.getType().getDescriptionId();
                    RewriteBootstrap.getInstance().getTitleProgressService()
                            .recordKill(state, getTitleRegistry(), entityId, true, mob.getUUID().toString());
                    if (state.isDirty()) {
                        com.kavinshi.playertitle.database.DatabaseAsyncWriter.queueWrite(
                                RewriteBootstrap.getInstance().getTitleRepository(), state);
                    }
                });
            }
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            // No longer saving NBT cache here. The capability object itself will be preserved during cloning.
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long currentTick = event.getServer().getTickCount();
        int progressCheckInterval = TitleConfig.SERVER.progressCheckInterval.get();
        boolean framePacing = TitleConfig.SERVER.enableFramePacing.get();

        if (currentTick - lastProgressCheckTick < progressCheckInterval) return;

        var players = event.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            lastProgressCheckTick = currentTick;
            return;
        }

        if (framePacing) {
            if (currentPlayerIndex >= players.size()) {
                currentPlayerIndex = 0;
            }

            ServerPlayer player = players.get(currentPlayerIndex);
            TitleCapability.get(player).ifPresent(state -> {
                long startTick = aliveTickMap.computeIfAbsent(player.getUUID(), k -> currentTick);
                int aliveMinutes = (int) ((currentTick - startTick) / 1200);
                if (aliveMinutes != state.getAliveMinutes()) {
                    RewriteBootstrap.getInstance().getTitleProgressService()
                            .recordAliveMinutes(state, getTitleRegistry(), aliveMinutes);
                }
                if (state.isDirty()) {
                    com.kavinshi.playertitle.database.DatabaseAsyncWriter.queueWrite(
                            RewriteBootstrap.getInstance().getTitleRepository(), state);
                }
            });

            currentPlayerIndex++;
            if (currentPlayerIndex >= players.size()) {
                lastProgressCheckTick = currentTick;
                currentPlayerIndex = 0;
            }
        } else {
            for (ServerPlayer player : players) {
                TitleCapability.get(player).ifPresent(state -> {
                    long startTick = aliveTickMap.computeIfAbsent(player.getUUID(), k -> currentTick);
                    int aliveMinutes = (int) ((currentTick - startTick) / 1200);
                    if (aliveMinutes != state.getAliveMinutes()) {
                        RewriteBootstrap.getInstance().getTitleProgressService()
                                .recordAliveMinutes(state, getTitleRegistry(), aliveMinutes);
                    }
                    if (state.isDirty()) {
                        com.kavinshi.playertitle.database.DatabaseAsyncWriter.queueWrite(
                                RewriteBootstrap.getInstance().getTitleRepository(), state);
                    }
                });
            }
            lastProgressCheckTick = currentTick;
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            LOGGER.debug("Player joined: {} ({})", playerId, player.getGameProfile().getName());

            aliveTickMap.put(playerId, (long) player.server.getTickCount());

            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    PlayerTitleState dbState = RewriteBootstrap.getInstance().getTitleRepository().loadPlayerState(playerId);
                    player.server.execute(() -> {
                        TitleCapability.get(player).ifPresent(state -> {
                            // Merge from dbState
                            state.setEquippedTitleId(dbState.getEquippedTitleId());
                            state.setAliveMinutes(dbState.getAliveMinutes());
                            state.setVersion(dbState.getVersion());
                            for (int titleId : dbState.getUnlockedTitleIds()) {
                                state.unlockTitleSilently(titleId);
                            }
                            state.setKillCountsSilently(dbState.getKillCounts());
                            state.updateLastLoadTime();
                            state.markClean();
                            
                            LOGGER.debug("Player data loaded from DB: {} unlocked titles, {} kill entries, equipped title: {}",
                                state.getUnlockedTitleIds().size(), state.getKillCounts().size(), state.getEquippedTitleId());
                            
                            syncPlayerData(player, state);
                            TitleSyncHandler.syncTitleRegistryToPlayer(player);
                            TitleSyncHandler.syncAllEquippedTitlesToPlayer(player);
                        });
                    });
                } catch (java.sql.SQLException e) {
                    LOGGER.error("Failed to load player state from DB on join for {}", playerId, e);
                }
            });

            if (!TitleCapability.get(player).isPresent()) {
                LOGGER.error("Player joined but TitleCapability is missing for {}", playerId);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        
        // Save state async
        TitleCapability.get(event.getEntity()).ifPresent(state -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    RewriteBootstrap.getInstance().getTitleRepository().savePlayerState(state);
                } catch (java.sql.SQLException e) {
                    LOGGER.error("Failed to save player state on leave for {}", uuid, e);
                }
            });
        });

        aliveTickMap.remove(uuid);
        RewriteBootstrap.getInstance().getRevisionService().removePlayer(uuid);
        com.kavinshi.playertitle.network.RequestSyncPacket.cleanupPlayer(uuid);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        UUID playerId = event.getOriginal().getUUID();
        LOGGER.debug("Player clone event for death: {}", playerId);

        TitleCapability.get(event.getOriginal()).ifPresent(oldState -> {
            TitleCapability.get(event.getEntity()).ifPresent(newState -> {
                // Copy state in-memory
                for (int titleId : oldState.getUnlockedTitleIds()) {
                    newState.unlockTitleSilently(titleId);
                }
                newState.setKillCountsSilently(oldState.getKillCounts());
                newState.setEquippedTitleId(oldState.getEquippedTitleId());
                newState.setVersion(oldState.getVersion());
                // Alive minutes reset on death
                newState.setAliveMinutes(0);

                int equippedId = oldState.getEquippedTitleId();
                if (equippedId >= 0) {
                    BuffHandler.applyBuffs((ServerPlayer) event.getEntity(), equippedId);
                }

                newState.markClean();
                LOGGER.debug("Copied title data for player {}: {} unlocked titles, equipped: {}",
                    playerId, newState.getUnlockedTitleIds().size(), newState.getEquippedTitleId());
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            aliveTickMap.put(player.getUUID(), (long) player.server.getTickCount());
            TitleCapability.get(player).ifPresent(state -> syncPlayerData(player, state));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TitleCapability.get(player).ifPresent(state -> syncPlayerData(player, state));
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
