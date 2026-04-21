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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
/**
 * 服务器事件处理器，负责处理玩家登录、死亡、Tick事件等服务器端事件。
 * 管理玩家标题数据的同步、进度检查和状态更新。
 */
public final class ServerEventHandler {

    private static final Map<java.util.UUID, Integer> aliveTickMap = new ConcurrentHashMap<>();
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
            // 分帧处理：每tick只处理一个玩家
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
            // 如果已经处理完所有玩家，更新检查时间
            if (currentPlayerIndex >= players.size()) {
                lastProgressCheckTick = currentTick;
                currentPlayerIndex = 0;
            }
        } else {
            // 传统模式：一次性处理所有玩家
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
            java.util.UUID playerId = player.getUUID();
            System.out.println("[TitleSystem] Player joined: " + playerId + " (" + player.getGameProfile().getName() + ")");
            
            aliveTickMap.put(playerId, player.server.getTickCount());
            TitleCapability.get(player).ifPresent(state -> {
                int unlockedCount = state.getUnlockedTitleIds().size();
                int killEntries = state.getKillCounts().size();
                int equippedId = state.getEquippedTitleId();
                System.out.println("[TitleSystem] Player data loaded: " + unlockedCount + " unlocked titles, " + killEntries + " kill entries, equipped title: " + equippedId);
                
                syncPlayerData(player, state);
            });
            
            if (!TitleCapability.get(player).isPresent()) {
                System.err.println("[TitleSystem] ERROR: Player joined but TitleCapability is missing!");
            }
            
            // Sync title registry and equipped titles to the player
            TitleSyncHandler.syncTitleRegistryToPlayer(player);
            TitleSyncHandler.syncAllEquippedTitlesToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        java.util.UUID uuid = event.getEntity().getUUID();
        aliveTickMap.remove(uuid);
        deathDataCache.remove(uuid);
    }

    private static final Map<java.util.UUID, CompoundTag> deathDataCache = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        java.util.UUID playerId = event.getOriginal().getUUID();
        System.out.println("[TitleSystem] Player clone event for death: " + playerId);

        CompoundTag savedNbt = deathDataCache.remove(playerId);
        if (savedNbt == null) {
            System.err.println("[TitleSystem] WARNING: No cached death data for player " + playerId);
            return;
        }

        PlayerTitleState oldState = new ForgePlayerTitleStateStore().read(playerId, savedNbt);

        TitleCapability.get(event.getEntity()).ifPresent(newState -> {
            int oldUnlockedCount = oldState.getUnlockedTitleIds().size();
            int oldKillEntries = oldState.getKillCounts().size();
            int oldEquippedId = oldState.getEquippedTitleId();

            System.out.println("[TitleSystem] Copying title data: " + oldUnlockedCount + " unlocked titles, " + oldKillEntries + " kill entries, equipped title: " + oldEquippedId);

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

            int newUnlockedCount = newState.getUnlockedTitleIds().size();
            int newKillEntries = newState.getKillCounts().size();
            int newEquippedId = newState.getEquippedTitleId();

            if (oldUnlockedCount != newUnlockedCount) {
                System.err.println("[TitleSystem] ERROR: Unlocked title count mismatch! Old: " + oldUnlockedCount + ", New: " + newUnlockedCount);
            } else {
                System.out.println("[TitleSystem] Successfully copied " + newUnlockedCount + " unlocked titles");
            }
            if (oldKillEntries != newKillEntries) {
                System.err.println("[TitleSystem] ERROR: Kill entries count mismatch! Old: " + oldKillEntries + ", New: " + newKillEntries);
            }
            if (oldEquippedId != newEquippedId) {
                System.err.println("[TitleSystem] ERROR: Equipped title ID mismatch! Old: " + oldEquippedId + ", New: " + newEquippedId);
            } else {
                System.out.println("[TitleSystem] Successfully copied equipped title ID: " + newEquippedId);
            }
        });
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            TitleCapability.get(player).ifPresent(state -> {
                CompoundTag nbt = new ForgePlayerTitleStateStore().write(state);
                deathDataCache.put(player.getUUID(), nbt);
                System.out.println("[TitleSystem] Cached death data for player " + player.getUUID() + " with " + state.getUnlockedTitleIds().size() + " unlocked titles");
            });
        }
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
