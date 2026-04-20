package com.kavinshi.playertitle.player;

import java.util.UUID;

public final class PlayerStateLifecycleHandler {
    public PlayerTitleState copyForClone(PlayerTitleState source, UUID newPlayerId, boolean wasDeath) {
        PlayerTitleState cloned = new PlayerTitleState(newPlayerId);

        for (int titleId : source.getUnlockedTitleIds()) {
            cloned.unlockTitle(titleId);
        }

        for (var entry : source.getKillCounts().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                cloned.addKill(entry.getKey());
            }
        }

        cloned.setEquippedTitleId(source.getEquippedTitleId());
        cloned.setAliveMinutes(wasDeath ? 0 : source.getAliveMinutes());
        cloned.markClean();
        return cloned;
    }
}
