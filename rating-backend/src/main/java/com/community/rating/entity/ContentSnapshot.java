package com.community.rating.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ContentSnapshot Entity: 内容及实时计数快照表
 * 已更新：使用 area_id (Integer) 替代 knowledge_tag (String)。
 */
@Data // 自动生成 Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // 自动生成无参构造函数
@AllArgsConstructor // 自动生成全参构造函数
@Entity
@Table(name = "contentsnapshot")
public class ContentSnapshot {

    /**
     * content_id: 帖子唯一 ID (Primary Key)
     */
    @Id
    @Column(name = "content_id")
    private Long contentId;

    /**
     * member_id: 内容作者 ID (Foreign Key)
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * publish_time: 发布时间 (用于 RecencyFactor 计算)
     */
    @Column(name = "publish_time", nullable = false)
    private LocalDateTime publishTime;

    /**
     * area_id: 帖子所属领域 ID (Foreign Key)
     */
    @Column(name = "area_id", nullable = false)
    private Integer areaId; // 核心变更：替换 knowledge_tag

    /**
     * post_length_level: 帖子长度分级 (1, 2, 3)
     */
    @Column(name = "post_length_level", nullable = false)
    private Integer postLengthLevel;

    /**
     * cis_score: CIS 最终计算结果 (核心输出)
     */
    @Column(name = "cis_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal cisScore;
    
    // --- 原始计数快照字段 (Snapshot Counts) ---

    @Column(name = "read_count_snapshot", nullable = false)
    private Integer readCountSnapshot;

    @Column(name = "like_count_snapshot", nullable = false)
    private Integer likeCountSnapshot;

    @Column(name = "comment_count_snapshot", nullable = false)
    private Integer commentCountSnapshot;

    @Column(name = "share_count_snapshot", nullable = false)
    private Integer shareCountSnapshot;

    @Column(name = "collect_count_snapshot", nullable = false)
    private Integer collectCountSnapshot;

    @Column(name = "hate_count_snapshot", nullable = false)
    private Integer hateCountSnapshot;
}