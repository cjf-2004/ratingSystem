package com.community.rating.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 对应数据库表 knowledgearea
 */
@Entity
@Table(name = "knowledgearea")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArea {

    /**
     * 领域唯一 ID (area_id INT Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "area_id")
    private Integer areaId;

    /**
     * 一级领域名称（area_name VARCHAR(50) NOT NULL, Unique）
     */
    @Column(name = "area_name", nullable = false, unique = true, length = 50)
    private String areaName;

    /**
     * 存储二级标签示例（sub_tags_list TEXT NOT NULL）
     */
    @Lob // 映射 TEXT/BLOB 类型
    @Column(name = "sub_tags_list", nullable = false)
    private String subTagsList;
}