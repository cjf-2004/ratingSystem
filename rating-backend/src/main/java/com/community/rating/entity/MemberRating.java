package com.community.rating.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.math.BigDecimal; // 导入 BigDecimal
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * MemberRating 实体类，对应数据库中的 MemberRating 表。
 * 使用 Lombok 注解 (@Data, @NoArgsConstructor, @AllArgsConstructor) 
 * 自动生成 Getter, Setter, toString, hashCode, equals 和构造函数，
 * 大大简化了代码。
 */
@Entity
@Table(name = "memberrating") // 明确指定表名
@Data // 自动生成 Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // 自动生成无参构造函数 (JPA 必需)
@AllArgsConstructor // 自动生成包含所有字段的构造函数 (可选，方便实例化)
public class MemberRating implements Serializable {

    // 数据库中的列名: rating_id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long ratingId;

    // 数据库中的列名: member_id
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 数据库中的列名: area_id
    @Column(name = "area_id")
    private Integer areaId; 

    // 数据库中的列名: des_score。使用 BigDecimal 匹配 DECIMAL(12, 4)
    @Column(name = "des_score", nullable = false, precision = 12, scale = 4)
    private BigDecimal desScore;

    // 数据库中的列名: rating_level
    @Column(name = "rating_level", nullable = false, length = 10)
    private String ratingLevel;
    
    // 数据库中的列名: update_date
    @Column(name = "update_date")
    private LocalDate updateDate;
    
    // 注意：所有的 Getter, Setter, 构造函数都由 Lombok 自动生成，不再需要手动编写。
}
