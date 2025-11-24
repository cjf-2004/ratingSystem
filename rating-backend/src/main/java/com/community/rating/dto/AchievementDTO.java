package com.community.rating.dto;

import lombok.Data;

@Data // 假设使用 Lombok
public class AchievementDTO {
    private Integer rank;              // 排名 (仅用于排名列表)
    private String achievementKey;    // 成就标识
    private String name;               // 成就名称
    private String description;        // 成就描述 (映射自 trigger_condition_desc)
    private String category;           // 成就分类 (映射自 type，但应进行更友好的展示映射)
    private Double completionRate;    // 成就完成率（0-1范围）
    private Integer achievedCount;     // 已达成该成就的成员数（用于排名）
}