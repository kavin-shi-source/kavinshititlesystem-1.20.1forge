package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.config.TitleConfig;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.SyncPlayerTitlesPacket;
import com.kavinshi.playertitle.player.ForgePlayerTitleStateStore;
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

    private static final Map<UUID, Integer> aliveTickMap = new ConcurrentHashMap<>();
    private static final Map<UUID, CompoundTag> deathDataCache = new ConcurrentHashMap<>();
    private static long lastProgressCheckTick = 0;
    private static int currentPlayerIndex = 0;

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

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            TitleCapability.get(player).ifPresent(state -> {
                CompoundTag nbt = new ForgePlayerTitleStateStore().write(state);
                deathDataCache.put(player.getUUID(), nbt);
                LOGGER.debug("Cached death data for player {} with {} unlocked titles",
                    player.getUUID(), state.getUnlockedTitleIds().size());
            });
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
                int startTick = aliveTickMap.computeIfAbsent(player.getUUID(), k -> (int) currentTick);
                int aliveMinutes = (int) ((currentTick - startTick) / 1200);
                if (aliveMinutes != state.getAliveMinutes()) {
                    RewriteBootstrap.getInstance().getTitleProgressService()
                            .recordAliveMinutes(state, getTitleRegistry(), aliveMinutes);
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
                    int startTick = aliveTickMap.computeIfAbsent(player.getUUID(), k -> (int) currentTick);
                    int aliveMinutes = (int) ((currentTick - startTick) / 1200);
                    if (aliveMinutes != state.getAliveMinutes()) {
                        RewriteBootstrap.getInstance().getTitleProgressService()
                                .recordAliveMinutes(state, getTitleRegistry(), aliveMinutes);
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

            aliveTickMap.put(playerId, player.server.getTickCount());
            TitleCapability.get(player).ifPresent(state -> {
                LOGGER.debug("Player data loaded: {} unlocked titles, {} kill entries, equipped title: {}",
                    state.getUnlockedTitleIds().size(), state.getKillCounts().size(), state.getEquippedTitleId());
                syncPlayerData(player, state);
            });

            if (!TitleCapability.get(player).isPresent()) {
                LOGGER.error("Player joined but TitleCapability is missing for {}", playerId);
            }

            TitleSyncHandler.syncTitleRegistryToPlayer(player);
            TitleSyncHandler.syncAllEquippedTitlesToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        aliveTickMap.remove(uuid);
        deathDataCache.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        UUID playerId = event.getOriginal().getUUID();
        LOGGER.debug("Player clone event for death: {}", playerId);

        CompoundTag savedNbt = deathDataCache.remove(playerId);
        if (savedNbt == null) {
            LOGGER.warn("No cached death data for player {}, attempting to read from original capability", playerId);
            TitleCapability.get(event.getOriginal()).ifPresent(oldState -> {
                CompoundTag fallbackNbt = new ForgePlayerTitleStateStore().write(oldState);
                copyTitleData(fallbackNbt, playerId, event);
            });
            return;
        }

        copyTitleData(savedNbt, playerId, event);
    }

    private static void copyTitleData(CompoundTag savedNbt, UUID playerId, PlayerEvent.Clone event) {
        PlayerTitleState oldState = new ForgePlayerTitleStateStore().read(playerId, savedNbt);

        TitleCapability.get(event.getEntity()).ifPresent(newState -> {
            for (int titleId : oldState.getUnlockedTitleIds()) {
                newState.unlockTitle(titleId);
            }
            newState.setKillCounts(oldState.getKillCounts());
            newState.setEquippedTitleId(oldState.getEquippedTitleId());
            newState.setAliveMinutes(0);

            var oldCustomTitle = oldState.getCustomTitle();
            newState.setCustomTitlePermission(oldCustomTitle.getPermission());
            if (oldCustomTitle.getPermission() > com.kavinshi.playertitle.title.CustomTitleData.PERMISSION_NONE) {
                newState.setCustomTitleText(oldCustomTitle.getText());
                newState.setCustomTitleColor1(oldCustomTitle.getColor1());
                if (oldCustomTitle.getPermission() >= com.kavinshi.playertitle.title.CustomTitleData.PERMISSION_GRADIENT) {
                    newState.setCustomTitleColor2(oldCustomTitle.getColor2());
                }
                newState.setUsingCustomTitle(oldCustomTitle.isUsingCustomTitle());
                newState.getCustomTitle().setLastModifiedTime(oldCustomTitle.getLastModifiedTime());
            }

            newState.markClean();
            LOGGER.debug("Copied title data for player {}: {} unlocked titles, equipped: {}",
                playerId, newState.getUnlockedTitleIds().size(), newState.getEquippedTitleId());
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            aliveTickMap.put(player.getUUID(), player.server.getTickCount());
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
