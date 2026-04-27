package com.kavinshi.playertitle.database;

import com.kavinshi.playertitle.player.PlayerTitleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseAsyncWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAsyncWriter.class);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PlayerTitle-DBWriter");
        t.setDaemon(true);
        return t;
    });

    public static void queueWrite(PlayerTitleRepository repository, PlayerTitleState state) {
        if (!state.isDirty()) return;

        CompletableFuture.runAsync(() -> {
            boolean success = false;
            int retries = 0;
            while (!success && retries < 3) {
                try {
                    repository.savePlayerState(state);
                    success = true;
                } catch (Exception e) {
                    retries++;
                    LOGGER.warn("Failed to write title data for player ***-***-*** (Attempt {}/3). Reason: {}", retries, e.getMessage());
                    if (retries < 3) {
                        try {
                            TimeUnit.SECONDS.sleep(1); // Wait 1s before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        LOGGER.error("Max retries reached. Dropping title data write for player ***-***-***.");
                    }
                }
            }
        }, EXECUTOR);
    }
}
