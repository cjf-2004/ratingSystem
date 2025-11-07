package com.community.rating.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ContentSnapshot Entity: 内容及实时计数快照表 (使用 Lombok 简化样板代码)
 * 对应数据库中的 'ContentSnapshot' 表结构。
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
     * area_id: 帖子所属领域
     */
    @Column(name = "area_id")
    private Integer areaId;

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

    /** read_count_snapshot: 原始阅读数快照 */
    @Column(name = "read_count_snapshot", nullable = false)
    private Integer readCountSnapshot;

    /** like_count_snapshot: 原始点赞数快照 */
    @Column(name = "like_count_snapshot", nullable = false)
    private Integer likeCountSnapshot;

    /** comment_count_snapshot: 原始评论数快照 */
    @Column(name = "comment_count_snapshot", nullable = false)
    private Integer commentCountSnapshot;

    /** share_count_snapshot: 原始转发数快照 */
    @Column(name = "share_count_snapshot", nullable = false)
    private Integer shareCountSnapshot;

    /** collect_count_snapshot: 原始收藏数快照 (用于 QualityFactor) */
    @Column(name = "collect_count_snapshot", nullable = false)
    private Integer collectCountSnapshot;

    /** hate_count_snapshot: 原始点踩数快照 (用于 NegativePenalty) */
    @Column(name = "hate_count_snapshot", nullable = false)
    private Integer hateCountSnapshot;
}