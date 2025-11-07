package com.community.rating.entity;

import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MemberRating Entity: 成员评级表
 * 已更新：使用 area_id (Integer) 替代 knowledge_tag (String)。
 * 主键调整为 rating_id (BIGINT) 并假设自增。
 */
@Entity
@Data
@Table(name = "memberrating")
public class MemberRating implements Serializable {

    /**
     * rating_id: 评级记录唯一 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 假设数据库自增
    @Column(name = "rating_id")
    private Long ratingId; 

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * area_id: 知识领域 ID (Foreign Key)
     */
    @Column(name = "area_id", nullable = false)
    private Integer areaId; // 核心变更：替换 knowledge_tag
    
    // 阶段二计算结果：领域专精度得分 (Domain Expertise Score)
    @Column(name = "des_score", nullable = false, precision = 12, scale = 4)
    private BigDecimal desScore;
    
    // 最终评级等级 (L1-L5)
    @Column(name = "rating_level", nullable = false, length = 2)
    private String ratingLevel;

    /**
     * update_date: 记录本次评级的计算日期。
     */
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate = LocalDateTime.now(); 
}