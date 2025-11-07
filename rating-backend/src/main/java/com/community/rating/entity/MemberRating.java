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
import java.time.LocalDate;

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

    // 数据库中的列名: area_id
    @Column(name = "area_id")
    private Integer areaId; 

    // 数据库中的列名: des_score。使用 BigDecimal 匹配 DECIMAL(12, 4)
    @Column(name = "des_score", nullable = false, precision = 12, scale = 4)
    private BigDecimal desScore;
    
    // 最终评级等级 (L1-L5)
    @Column(name = "rating_level", nullable = false, length = 2)
    private String ratingLevel;
    
    // 数据库中的列名: update_date
    @Column(name = "update_date")
    private LocalDate updateDate;
    
    // 注意：所有的 Getter, Setter, 构造函数都由 Lombok 自动生成，不再需要手动编写。
}
