// com.community.rating.dto.ContentDTO.java
package com.community.rating.dto;

import lombok.Data;

@Data
public class ContentDTO {
    private Integer rank;        // 排名
    private Long contentId;     // 内容ID
    private Long authorId;      // 作者ID
    private String authorName;  // 作者姓名
    private String publishTime; // 发布时间（ISO 8601格式，UTC）
    private String domain;       // 所属领域
    private Integer score;       // 内容分数 (映射自 cis_score)
    private Integer likes;       // 点赞数
    private Integer unlikes;     // 点踩数 (映射自 hate_count_snapshot)
    private Integer comments;    // 评论数
    private Integer shares;      // 分享数
}