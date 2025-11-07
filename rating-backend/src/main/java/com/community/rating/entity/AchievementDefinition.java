package com.community.rating.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 对应数据库表 achievementdefinition
 */
@Entity
@Table(name = "achievementdefinition")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDefinition {

    /**
     * 成就的唯一 KEY (achievement_key VARCHAR(50) Primary Key)
     */
    @Id
    @Column(name = "achievement_key", length = 50)
    private String achievementKey;

    /**
     * 成就的展示名称 (name VARCHAR(100) NOT NULL)
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 成就类型 (type VARCHAR(20) NOT NULL)
     */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /**
     * 成就触发条件的详细描述 (trigger_condition_desc TEXT NOT NULL)
     */
    @Lob // 映射 TEXT/BLOB 类型
    @Column(name = "trigger_condition_desc", nullable = false)
    private String triggerConditionDesc;
}