package com.kavinshi.playertitle.title;

/**
 * 标题条件类型枚举，定义解锁称号所需满足的条件类型。
 * 包括击杀生物、生存时间、赏金等多种条件。
 */
public enum TitleConditionType {
    KILL_ANY_HOSTILE,
    KILL_MOB,
    KILL_UUID,
    SURVIVAL_TIME,
    BOUNTY
}
