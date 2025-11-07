package com.community.rating.dto;

import lombok.Data;
import java.util.List;

/**
 * 系统概览数据传输对象
 */
@Data
public class SystemOverviewDTO {
    // 最后更新时间（ISO 8601格式，UTC）
    private String lastUpdateTime;
    
    // 总成员数
    private Integer totalMembers;
    
    // 总内容数
    private Integer totalContents;
    
    // 活跃成员数
    private Integer activeMembers;
    
    // 今日新增内容数
    private Integer newContentsToday;
    
    // 今日新增成就数
    private Integer newAchievementsToday;
    
    // 平均等级（如"L2.8"）
    private String averageRating;
    
    // 顶级成员列表
    private List<TopMemberDTO> topMembers;
    
    // 顶级成就列表
    private List<TopAchievementDTO> topAchievements;
}