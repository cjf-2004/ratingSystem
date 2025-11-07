// File: ContentSnapshotRepository.java
package com.community.rating.repository;

import com.community.rating.entity.ContentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository; // 导入 Repository
import java.time.LocalDateTime;

import java.util.List;

@Repository // 添加 @Repository
public interface ContentSnapshotRepository extends JpaRepository<ContentSnapshot, Long> {

    // ContentSnapshot (单表): countByPublishTimeAfter()
    Long countByPublishTimeAfter(LocalDateTime time);

    // ContentSnapshot (单表/跨字段统计): countDistinctMemberIdByPublishTimeAfter()
    @Query(value = "SELECT COUNT(DISTINCT c.member_id) FROM contentsnapshot c WHERE c.publish_time > :time", nativeQuery = true)
    Long countDistinctMemberIdByPublishTimeAfter(LocalDateTime time);

    /**
     * 用于 DES 计算：获取所有已计算 CIS 结果的内容。
     * @return 包含 CIS 分数的所有内容快照实体
     */
    List<ContentSnapshot> findAllByCisScoreIsNotNull();
}