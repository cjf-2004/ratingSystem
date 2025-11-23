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

    // 联查所有成就的定义信息，并 LEFT JOIN 统计达成人数。
    @Query(value = "SELECT " +
               "    d.achievement_key, d.name, d.type, d.trigger_condition_desc, " +
               "    COUNT(DISTINCT a.member_id) AS achieved_count " +
               "FROM achievementdefinition d " +
               "LEFT JOIN achievementstatus a ON d.achievement_key = a.achievement_key " + // 使用 LEFT JOIN 保证所有定义都返回
               "GROUP BY d.achievement_key, d.name, d.type, d.trigger_condition_desc", nativeQuery = true)
    List<Object[]> findAllAchievementsWithStats();
}