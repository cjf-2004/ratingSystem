package com.community.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 成员领域专精度得分 (DES) 和最终评级结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberRatingDTO {
    // 成员ID
    private Long memberId;
    // 知识领域标签
    private String knowledgeTag;
    // 领域专精度得分 (DES)
    private BigDecimal desScore;
    // 最终评级等级
    private String ratingLevel;
}