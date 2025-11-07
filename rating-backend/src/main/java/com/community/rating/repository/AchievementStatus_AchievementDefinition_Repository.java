// File: AchievementStatusAchievementDefinitionRepository.java
package com.community.rating.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository; // 导入 Repository
import java.util.List;
import com.community.rating.entity.AchievementStatus;

@Repository // 添加 @Repository
public interface AchievementStatus_AchievementDefinition_Repository extends org.springframework.data.repository.Repository<AchievementStatus, Long> {
    
    // 联合查询: findTopAchievementsStats()
    @Query(value = "SELECT " +
                   "  a.achievement_key, " +
                   "  d.name, " +
                   "  COUNT(DISTINCT a.member_id) " +
                   "FROM achievementstatus a JOIN achievementdefinition d ON a.achievement_key = d.achievement_key " +
                   "GROUP BY a.achievement_key, d.name " +
                   "ORDER BY COUNT(DISTINCT a.member_id) DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopAchievementsStats(int limit);
}