package com.kavinshi.playertitle.icon;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 图标扫描管理器，负责监控图标目录变化并自动重新加载图标。
 * 提供文件系统监控和定期扫描功能，确保图标定义与文件系统同步。
 */
public class IconScanner {
    private final IconManager iconManager;
    private final Path watchDirectory;
    private final AtomicBoolean isWatching = new AtomicBoolean(false);
    private WatchService watchService;
    private ScheduledExecutorService scheduler;
    
    /**
     * 创建图标扫描管理器。
     *
     * @param iconManager 图标管理器实例
     * @param watchDirectory 要监控的目录（通常为config/titlesystem/icons/）
     */
    public IconScanner(IconManager iconManager, Path watchDirectory) {
        this.iconManager = iconManager;
        this.watchDirectory = watchDirectory;
    }
    
    /**
     * 启动目录监控服务。
     * 开始监控文件系统事件并定期检查目录变化。
     *
     * @throws IOException 如果监控服务启动失败
     */
    public void startWatching() throws IOException {
        if (isWatching.compareAndSet(false, true)) {
            // 创建监控服务
            watchService = FileSystems.getDefault().newWatchService();
            watchDirectory.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            
            // 创建调度器进行定期扫描（每30秒一次，作为监控的备份）
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "IconScanner-WatchThread");
                thread.setDaemon(true);
                return thread;
            });
            
            // 启动监控线程
            scheduler.execute(this::watchLoop);
            
            // 启动定期扫描（防止监控事件丢失）
            scheduler.scheduleAtFixedRate(this::safeScan, 30, 30, TimeUnit.SECONDS);
            
            System.out.println("图标扫描监控已启动，监控目录: " + watchDirectory);
        }
    }
    
    /**
     * 停止目录监控服务。
     */
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
                    System.err.println("关闭监控服务时出错: " + e.getMessage());
                }
            }
            
            System.out.println("图标扫描监控已停止");
        }
    }
    
    /**
     * 手动触发图标扫描。
     *
     * @throws IOException 如果扫描过程中发生错误
     */
    public void scanNow() throws IOException {
        iconManager.scanIcons();
        System.out.println("手动图标扫描完成");
    }
    
    /**
     * 监控循环，处理文件系统事件。
     */
    private void watchLoop() {
        try {
            while (isWatching.get()) {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
                }
                
                boolean shouldRescan = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // 忽略溢出事件
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    // 检查是否是PNG文件
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    if (fileName.toString().toLowerCase().endsWith(".png")) {
                        shouldRescan = true;
                        System.out.println("检测到PNG文件变化: " + fileName + " (" + kind + ")");
                    }
                }
                
                if (shouldRescan) {
                    safeScan();
                }
                
                // 重置监控键
                boolean valid = key.reset();
                if (!valid) {
                    System.err.println("监控键无效，目录可能已删除");
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("监控循环被中断");
        } catch (ClosedWatchServiceException e) {
            // 正常关闭，忽略
        } catch (Exception e) {
            System.err.println("监控循环发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 安全的图标扫描，捕获并记录异常。
     */
    private void safeScan() {
        try {
            iconManager.scanIcons();
            System.out.println("定期图标扫描完成，当前图标数: " + iconManager.getAllIcons().size());
        } catch (IOException e) {
            System.err.println("图标扫描失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查监控服务是否正在运行。
     *
     * @return 如果监控正在运行返回true
     */
    public boolean isWatching() {
        return isWatching.get();
    }
    
    /**
     * 获取当前监控的目录。
     *
     * @return 监控目录路径
     */
    public Path getWatchDirectory() {
        return watchDirectory;
    }
}