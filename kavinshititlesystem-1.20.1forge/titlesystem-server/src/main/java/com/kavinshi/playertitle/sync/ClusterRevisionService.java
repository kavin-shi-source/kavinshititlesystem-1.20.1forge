package com.kavinshi.playertitle.sync;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 集群事件版本控制服务。
 * 为每个玩家维护单调递增的版本号，确保跨服事件的顺序和幂等性。
 * 版本号用于防止事件重复处理和处理顺序混乱。
 */
public class ClusterRevisionService {
    private final Map<UUID, AtomicLong> playerRevisions = new ConcurrentHashMap<>();
    
    /**
     * 获取玩家当前版本号。
     * 如果玩家不存在，返回0。
     *
     * @param playerId 玩家UUID
     * @return 当前版本号，如果玩家不存在返回0
     */
    public long getCurrentRevision(UUID playerId) {
        AtomicLong revision = playerRevisions.get(playerId);
        return revision == null ? 0 : revision.get();
    }
    
    /**
     * 获取下一个版本号并递增。
     * 如果玩家不存在，创建新记录并从1开始。
     *
     * @param playerId 玩家UUID
     * @return 递增后的版本号
     */
    public long getAndIncrement(UUID playerId) {
        return playerRevisions
            .computeIfAbsent(playerId, k -> new AtomicLong(0))
            .getAndIncrement();
    }
    
    /**
     * 递增版本号并返回新值。
     * 如果玩家不存在，创建新记录并从1开始。
     *
     * @param playerId 玩家UUID
     * @return 递增后的版本号
     */
    public long incrementAndGet(UUID playerId) {
        return playerRevisions
            .computeIfAbsent(playerId, k -> new AtomicLong(0))
            .incrementAndGet();
    }
    
    /**
     * 更新玩家版本号（仅当新版本号大于当前版本号时）。
     * 用于从其他服务器同步版本号。
     *
     * @param playerId 玩家UUID
     * @param newRevision 新版本号
     * @return 如果更新成功返回true，如果新版本号不大于当前版本号返回false
     */
    public boolean updateRevisionIfGreater(UUID playerId, long newRevision) {
        AtomicLong current = playerRevisions.computeIfAbsent(playerId, k -> new AtomicLong(0));
        
        while (true) {
            long currentValue = current.get();
            if (newRevision <= currentValue) {
                return false; // 新版本号不大于当前版本号
            }
            if (current.compareAndSet(currentValue, newRevision)) {
                return true; // 成功更新
            }
            // CAS失败，重试
        }
    }
    
    /**
     * 重置玩家版本号。
     * 警告：仅在玩家数据重置时使用。
     *
     * @param playerId 玩家UUID
     */
    public void resetRevision(UUID playerId) {
        playerRevisions.put(playerId, new AtomicLong(0));
    }
    
    /**
     * 移除玩家版本记录。
     * 当玩家数据被永久删除时使用。
     *
     * @param playerId 玩家UUID
     */
    public void removePlayer(UUID playerId) {
        playerRevisions.remove(playerId);
    }
    
    /**
     * 检查版本号是否有效（大于0）。
     *
     * @param playerId 玩家UUID
     * @param revision 版本号
     * @return 如果版本号有效（大于0且大于等于当前版本号）返回true
     */
    public boolean isValidRevision(UUID playerId, long revision) {
        if (revision <= 0) {
            return false;
        }
        long current = getCurrentRevision(playerId);
        return revision >= current;
    }
    
    /**
     * 获取所有玩家的版本号快照。
     * 用于调试和监控。
     *
     * @return 玩家版本号映射的副本
     */
    public Map<UUID, Long> getAllRevisions() {
        Map<UUID, Long> snapshot = new ConcurrentHashMap<>();
        playerRevisions.forEach((playerId, revision) -> 
            snapshot.put(playerId, revision.get()));
        return snapshot;
    }
    
    /**
     * 清除所有版本记录。
     * 警告：仅在服务器重启或维护时使用。
     */
    public void clearAll() {
        playerRevisions.clear();
    }
    
    /**
     * 获取活跃玩家数量。
     *
     * @return 有版本记录的玩家数量
     */
    public int getPlayerCount() {
        return playerRevisions.size();
    }
}