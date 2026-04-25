package com.kavinshi.playertitle.icon;

import com.kavinshi.playertitle.config.TitleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 图标扫描器，负责监控图标目录的变化并自动重新扫描图标。
 * 使用文件系统WatchService监控目录变化，并定期执行扫描任务。
 */
public class IconScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(IconScanner.class);
    private final IconManager iconManager;
    private final Path watchDirectory;
    private final AtomicBoolean isWatching = new AtomicBoolean(false);
    private WatchService watchService;
    private ScheduledExecutorService scheduler;

    public IconScanner(IconManager iconManager, Path watchDirectory) {
        this.iconManager = iconManager;
        this.watchDirectory = watchDirectory;
    }

    public void startWatching() throws IOException {
        if (isWatching.compareAndSet(false, true)) {
            watchService = FileSystems.getDefault().newWatchService();
            watchDirectory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "IconScanner-WatchThread");
                thread.setDaemon(true);
                return thread;
            });
            scheduler.execute(this::watchLoop);
            int scanInterval = TitleConfig.SERVER.iconScanInterval.get();
            scheduler.scheduleAtFixedRate(this::safeScan, scanInterval, scanInterval, TimeUnit.SECONDS);
        }
    }

    public void stopWatching() {
        if (isWatching.compareAndSet(true, false)) {
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close watch service: {}", e.getMessage());
                }
            }
        }
    }

    public void scanNow() throws IOException {
        iconManager.scanIcons();
    }

    private void watchLoop() {
        try {
            while (isWatching.get()) {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) continue;
                boolean shouldRescan = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    if (fileName.toString().toLowerCase().endsWith(".png")) {
                        shouldRescan = true;
                    }
                }
                if (shouldRescan) safeScan();
                if (!key.reset()) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException e) {
        } catch (Exception e) {
            LOGGER.error("IconScanner watch loop error: {}", e.getMessage());
        }
    }

    private void safeScan() {
        try {
            iconManager.scanIcons();
        } catch (IOException e) {
            LOGGER.error("Icon scan failed: {}", e.getMessage());
        }
    }

    public boolean isWatching() { return isWatching.get(); }
    public Path getWatchDirectory() { return watchDirectory; }
}
