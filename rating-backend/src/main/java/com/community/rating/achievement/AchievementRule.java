package com.community.rating.achievement;

import java.util.List;

/**
 * 成就规则接口。每个实现类负责检测一种成就，并返回满足条件的成员 ID 列表。
 */
public interface AchievementRule {
    /**
     * 成就唯一 key（应与数据库 AchievementDefinition.achievement_key 对应）
     */
    String getAchievementKey();

    /**
     * 执行检测并返回新达到该成就的成员 ID 列表（不包含已发放过的）。
     */
    List<Long> detect();
}
