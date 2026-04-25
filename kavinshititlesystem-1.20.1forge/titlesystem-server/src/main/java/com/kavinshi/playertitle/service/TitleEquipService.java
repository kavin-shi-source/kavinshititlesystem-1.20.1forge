package com.kavinshi.playertitle.service;

import com.kavinshi.playertitle.player.PlayerTitleState;
import com.kavinshi.playertitle.sync.ClusterEventBus;
import com.kavinshi.playertitle.sync.TitleEventFactory;
import com.kavinshi.playertitle.title.TitleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * 称号装备服务，处理玩家装备和卸下称号的逻辑。
 * 发布装备状态变更事件到集群事件总线。
 */
public final class TitleEquipService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TitleEquipService.class);
    private final TitleEventFactory eventFactory;
    private final ClusterEventBus eventBus;
    
    /**
     * 创建称号装备服务。
     *
     * @param eventFactory 事件工厂
     * @param eventBus 事件总线
     */
    public TitleEquipService(TitleEventFactory eventFactory, ClusterEventBus eventBus) {
        this.eventFactory = eventFactory;
        this.eventBus = eventBus;
    }
    
    /**
     * 装备称号。
     *
     * @param state 玩家称号状态
     * @param registry 称号注册表
     * @param titleId 要装备的称号ID
     * @return 装备结果
     */
    public EquipResult equip(PlayerTitleState state, TitleRegistry registry, int titleId) {
        if (registry.getTitle(titleId) == null) {
            return new EquipResult(false, "TITLE_NOT_FOUND", state.getEquippedTitleId());
        }

        if (!state.isTitleUnlocked(titleId)) {
            return new EquipResult(false, "TITLE_NOT_UNLOCKED", state.getEquippedTitleId());
        }

        if (state.getEquippedTitleId() == titleId) {
            return new EquipResult(true, "ALREADY_EQUIPPED", titleId);
        }

        state.setEquippedTitleId(titleId);
        
        // 发布装备事件
        try {
            var event = eventFactory.createEquipEvent(state.getPlayerId(), titleId, Instant.now());
            eventBus.publish(event);
        } catch (Exception e) {
            // 事件发布失败不应影响装备操作，但应记录日志
            LOGGER.error("Failed to publish equip event: {}", e.getMessage());
        }
        
        return new EquipResult(true, "EQUIPPED", titleId);
    }

    /**
     * 卸下称号。
     *
     * @param state 玩家称号状态
     * @return 卸下结果
     */
    public EquipResult unequip(PlayerTitleState state) {
        int previousTitleId = state.getEquippedTitleId();
        if (previousTitleId == -1) {
            return new EquipResult(true, "ALREADY_UNEQUIPPED", -1);
        }

        state.setEquippedTitleId(-1);
        
        // 发布卸下事件
        try {
            var event = eventFactory.createUnequipEvent(state.getPlayerId(), previousTitleId, Instant.now());
            eventBus.publish(event);
        } catch (Exception e) {
            // 事件发布失败不应影响卸下操作，但应记录日志
            LOGGER.error("Failed to publish unequip event: {}", e.getMessage());
        }
        
        return new EquipResult(true, "UNEQUIPPED", -1);
    }
}