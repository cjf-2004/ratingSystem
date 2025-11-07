package com.community.rating.repository;

import com.community.rating.entity.KnowledgeArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KnowledgeAreaRepository extends JpaRepository<KnowledgeArea, Integer> {

    /**
     * 根据领域名称（即 knowledgeTag）查找 KnowledgeArea 实体，以获取 areaId。
     * @param areaName 领域名称 (tag)
     * @return 匹配的 KnowledgeArea 实体
     */
    Optional<KnowledgeArea> findByAreaName(String areaName);
}