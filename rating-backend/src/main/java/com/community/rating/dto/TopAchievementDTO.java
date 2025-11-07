package com.community.rating.dto;

import lombok.Data;

/**
 * 顶级成就统计信息 DTO
 */
@Data
public class TopAchievementDTO {
    // 排名（从1开始）
    private Integer rank;
    
    // 成就ID
    private String achievementKey;
    
    // 成就名称
    private String achievementName;
    
    // 成就总数 (独立达成成员数)
    private Long achievementCount;
    
    // 成就完成率（0-1范围，如0.96）
    private Double completionRate; 
}