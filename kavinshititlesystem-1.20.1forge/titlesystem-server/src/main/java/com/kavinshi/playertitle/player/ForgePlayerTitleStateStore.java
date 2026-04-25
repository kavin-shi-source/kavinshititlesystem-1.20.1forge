package com.kavinshi.playertitle.player;

import com.kavinshi.playertitle.title.CustomTitleData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

@SuppressWarnings("null")
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

        CustomTitleData ct = state.getCustomTitle();
        CompoundTag ctTag = new CompoundTag();
        ctTag.putString("text", ct.getText());
        ctTag.putInt("permission", ct.getPermission());
        ctTag.putInt("color1", ct.getColor1());
        ctTag.putInt("color2", ct.getColor2());
        ctTag.putBoolean("usingCustomTitle", ct.isUsingCustomTitle());
        ctTag.putLong("lastModifiedTime", ct.getLastModifiedTime());
        root.put("customTitle", ctTag);

        return root;
    }

    public PlayerTitleState read(UUID playerId, CompoundTag tag) {
        PlayerTitleState state = new PlayerTitleState(playerId);

        for (int titleId : tag.getIntArray("unlockedTitles")) {
            state.unlockTitle(titleId);
        }

        Map<String, Integer> killCounts = new HashMap<>();
        CompoundTag killCountsTag = tag.getCompound("killCounts");
        for (String key : killCountsTag.getAllKeys()) {
            int count = killCountsTag.getInt(key);
            if (count > 0) {
                killCounts.put(key, count);
            }
        }
        state.setKillCounts(killCounts);

        state.setAliveMinutes(tag.getInt("aliveMinutes"));
        state.setEquippedTitleId(tag.getInt("equippedTitleId"));

        if (tag.contains("customTitle")) {
            CompoundTag ctTag = tag.getCompound("customTitle");
            state.setCustomTitlePermission(ctTag.getInt("permission"));
            state.setCustomTitleText(ctTag.getString("text"));
            state.setCustomTitleColor1(ctTag.getInt("color1"));
            state.setCustomTitleColor2(ctTag.getInt("color2"));
            state.setUsingCustomTitle(ctTag.getBoolean("usingCustomTitle"));
            state.getCustomTitle().setLastModifiedTime(ctTag.getLong("lastModifiedTime"));
        }

        state.markClean();
        return state;
    }
}
