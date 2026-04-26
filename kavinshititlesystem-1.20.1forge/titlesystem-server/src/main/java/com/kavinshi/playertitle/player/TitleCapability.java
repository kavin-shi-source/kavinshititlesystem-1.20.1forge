package com.kavinshi.playertitle.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TitleCapability {
    public static final Capability<PlayerTitleState> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    public static LazyOptional<PlayerTitleState> get(net.minecraft.world.entity.player.Player player) {
        return player.getCapability(CAPABILITY);
    }

    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerTitleState data;
        private final LazyOptional<PlayerTitleState> lazyData;

        public Provider(UUID playerId) {
            this.data = new PlayerTitleState(playerId);
            this.lazyData = LazyOptional.of(() -> this.data);
        }

        public PlayerTitleState getData() {
            return data;
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == CAPABILITY) {
                return lazyData.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return new ForgePlayerTitleStateStore().write(data);
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            UUID playerId = data.getPlayerId();
            PlayerTitleState restored = new ForgePlayerTitleStateStore().read(playerId, nbt);

            for (int titleId : restored.getUnlockedTitleIds()) {
                data.unlockTitle(titleId);
            }
            data.setKillCounts(restored.getKillCounts());
            data.setAliveMinutes(restored.getAliveMinutes());
            data.setEquippedTitleId(restored.getEquippedTitleId());
            data.markClean();
        }
    }
}
