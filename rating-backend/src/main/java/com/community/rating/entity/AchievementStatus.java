package com.community.rating.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievementstatus")
@Data // 自动生成 Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // 自动生成无参构造函数
@AllArgsConstructor // 自动生成全参构造函数
public class AchievementStatus {

    /**
     * 状态记录唯一 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long statusId;

    /**
     * 达成者 ID (Foreign Key)
     */
    @Column(name = "member_id")
    private Long memberId;

    /**
     * 成就的唯一 KEY (Foreign Key)
     */
    @Column(name = "achievement_key", length = 50)
    private String achievementKey;

    /**
     * 即时记录达成时间（来自通知）(NOT NULL)
     */
    @Column(name = "achieved_time", nullable = false)
    private LocalDateTime achievedTime;
}