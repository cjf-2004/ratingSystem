package com.community.rating.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Member Entity: 成员基础信息表 (使用 Lombok 简化样板代码)
 * 对应数据库中的 'Member' 表结构。
 */
@Data // 自动生成 Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // 自动生成无参构造函数
@AllArgsConstructor // 自动生成全参构造函数
@Entity
@Table(name = "member")
public class Member {

    /**
     * member_id: 成员唯一标识符 (Primary Key)
     * 对应数据库 BIGINT 类型。
     */
    @Id
    @Column(name = "member_id")
    private Long memberId;

    /**
     * name: 成员昵称
     * 对应数据库 VARCHAR(100) NOT NULL。
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * join_date: 加入日期 (用于时间性成就)
     * 对应数据库 TIMESTAMP NOT NULL。
     */
    @Column(name = "join_date", nullable = false)
    private LocalDateTime joinDate;
}