package com.community.rating.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内容数据传输对象，用于在服务层之间传递计算所需的数据。
 * 已更新：使用 areaId (Integer) 替代 knowledgeTag (String)。
 */
@Data
public class ContentDataDTO {
    private Long contentId;
    private Long memberId;
    
    // 核心变更：使用 areaId
    private Integer areaId; 
    
    private LocalDateTime publishTime;
    
    // 快照数据 (使用 Long 以保证足够的数值范围)
    private Long readCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Long collectCount;
    private Long hateCount;
    private Integer postLengthLevel;
    
    // 暂存计算结果
    private BigDecimal cisScore = BigDecimal.ZERO;
}