package com.community.rating.dto;

import lombok.Data;

/**
 * 顶级成员排名信息 DTO
 */
@Data
public class TopMemberDTO {
    // 排名（从顶开始）
    private Integer rank;
    
    // 成员ID
    private Long memberId;
    
    // 成员姓名
    private String memberName;
    
    // 主要领域（如"Web开发"）
    private String mainDomain;
    
    // 等级（如"L5"）
    private String level;
    
    // 影响力分数（从 DECIMAL 转换后的整数）
    private Integer score;
}