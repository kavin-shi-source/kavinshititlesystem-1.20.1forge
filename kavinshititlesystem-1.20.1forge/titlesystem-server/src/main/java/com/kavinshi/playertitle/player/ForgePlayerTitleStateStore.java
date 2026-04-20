package com.kavinshi.playertitle.player;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public final class ForgePlayerTitleStateStore {
    public CompoundTag write(PlayerTitleState state) {
        CompoundTag root = new CompoundTag();
        root.putIntArray("unlockedTitles", state.getUnlockedTitleIds().stream().sorted().mapToInt(Integer::intValue).toArray());
        root.putInt("equippedTitleId", state.getEquippedTitleId());
        root.putInt("aliveMinutes", state.getAliveMinutes());

        CompoundTag killCountsTag = new CompoundTag();
        for (var entry : state.getKillCounts().entrySet()) {
            killCountsTag.putInt(entry.getKey(), entry.getValue());
        }
        root.put("killCounts", killCountsTag);
        return root;
    }

    public PlayerTitleState read(UUID playerId, CompoundTag tag) {
        PlayerTitleState state = new PlayerTitleState(playerId);

        for (int titleId : tag.getIntArray("unlockedTitles")) {
            state.unlockTitle(titleId);
        }

        CompoundTag killCountsTag = tag.getCompound("killCounts");
        for (String key : killCountsTag.getAllKeys()) {
            int count = killCountsTag.getInt(key);
            for (int i = 0; i < count; i++) {
                state.addKill(key);
            }
        }

        state.setAliveMinutes(tag.getInt("aliveMinutes"));
        state.setEquippedTitleId(tag.getInt("equippedTitleId"));
        state.markClean();
        return state;
    }
}
