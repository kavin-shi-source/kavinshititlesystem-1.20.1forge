package com.kavinshi.playertitle.service;

import com.kavinshi.playertitle.database.DatabaseAsyncWriter;
import com.kavinshi.playertitle.database.PlayerTitleRepository;
import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.title.TitleRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlayerDataSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataSyncService.class);

    private final PlayerTitleRepository repository;
    private final TitleProgressService progressService;
    private final TitleRegistry titleRegistry;

    public PlayerDataSyncService(PlayerTitleRepository repository,
                                  TitleProgressService progressService,
                                  TitleRegistry titleRegistry) {
        this.repository = repository;
        this.progressService = progressService;
        this.titleRegistry = titleRegistry;
    }

    public void handleProgressUpdate(ServerPlayer player, ProgressUpdater updater) {
        TitleCapability.get(player).ifPresent(state -> {
            updater.update(state, progressService, titleRegistry);
            syncIfDirty(state);
        });
    }

    private void syncIfDirty(PlayerTitleState state) {
        if (state.isDirty()) {
            DatabaseAsyncWriter.queueWrite(repository, state);
        }
    }

    @FunctionalInterface
    public interface ProgressUpdater {
        void update(PlayerTitleState state, TitleProgressService service, TitleRegistry registry);
    }
}
