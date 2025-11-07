package com.community.rating.repository;

import com.community.rating.entity.ContentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentSnapshotRepository extends JpaRepository<ContentSnapshot, Long> {

    /**
     * 用于 DES 计算：获取所有已计算 CIS 结果的内容。
     * @return 包含 CIS 分数的所有内容快照实体
     */
    List<ContentSnapshot> findAllByCisScoreIsNotNull();
}