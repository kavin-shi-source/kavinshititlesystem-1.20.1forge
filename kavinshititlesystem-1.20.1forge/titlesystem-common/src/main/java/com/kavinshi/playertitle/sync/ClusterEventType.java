package com.kavinshi.playertitle.sync;

/**
 * 跨服事件类型枚举，定义可在不同服务器之间同步的事件类型。
 * 每个事件类型对应一个具体的业务操作，用于Redis Pub/Sub消息分发。
 */
public enum ClusterEventType {
    /** 玩家获得新称号 */
    TITLE_ASSIGNED,
    
    /** 玩家移除称号 */
    TITLE_REMOVED,
    
    /** 玩家称号更新（图标、颜色、样式等变更） */
    TITLE_UPDATED,
    
    /** 玩家称号进度更新 */
    TITLE_PROGRESS_UPDATED,
    
    /** 玩家称号装备状态变更 */
    TITLE_EQUIP_STATE_CHANGED,
    
    /** 服务器公告（跨服消息） */
    SERVER_ANNOUNCEMENT
}