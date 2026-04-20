package com.kavinshi.playertitle.player;

import java.util.UUID;

public interface PlayerTitleStateRepository {
    PlayerTitleState getOrCreate(UUID playerId);

    void save(PlayerTitleState state);

    void remove(UUID playerId);
}
