package com.community.rating.dto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 成员分领域的历史分数记录项。
 * 领域信息由外部 MemberScoreHistoryDTO 实例的 main_domain 字段提供。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreHistoryItemDTO {
    
    // 记录本次评级的计算日期 (格式化为字符串)
    private String update_date; 
    
    // DESK 最终影响力分数
    private Double des_score;   
}