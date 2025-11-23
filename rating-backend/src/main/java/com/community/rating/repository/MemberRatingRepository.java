package com.community.rating.repository;

import com.community.rating.entity.MemberRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRatingRepository extends JpaRepository<MemberRating, Long> {

    /**
     * 根据知识领域 ID 查询该领域下所有成员的评级，并按分数降序排列（用于排名榜单）
     * 对应 SQL: SELECT * FROM member_rating WHERE knowledge_area_id = ? ORDER BY des_score DESC
     */
    List<MemberRating> findByAreaIdOrderByDesScoreDesc(Integer areaId);

    /**
     * 根据成员 ID 查询其在所有领域的评级
     */
    List<MemberRating> findByMemberId(Long memberId);

    /**
     * 用于检查和更新特定成员/领域的评级记录 (Find or Create 场景)。
     * @param memberId 成员 ID
     * @param areaId 领域 ID
     * @return 匹配的评级记录
     */
    Optional<MemberRating> findByMemberIdAndAreaId(Long memberId, Integer areaId);

    /**
     * 用于获取特定成员的所有领域评级（如果有需要）。
     */
    List<MemberRating> findAllByMemberId(Long memberId);
    // MemberRating (单表): findMaxUpdateDate()
    @Query("SELECT MAX(m.updateDate) FROM MemberRating m")
    LocalDate findMaxUpdateDate();

    // MemberRating (单表/复杂查询): calculateAverageDesScoreOfLatestRatings()
    @Query(value = "SELECT AVG(latest_ratings.des_score) FROM (" +
                   "    SELECT mr.des_score, " +
                   "           ROW_NUMBER() OVER(PARTITION BY mr.member_id ORDER BY mr.update_date DESC) as rn " +
                   "    FROM memberrating mr" +
                   ") latest_ratings " +
                   "WHERE latest_ratings.rn = 1", nativeQuery = true)
    Double calculateAverageDesScoreOfLatestRatings();

    // Return member_ids who have at least `minAreas` distinct areas with rating level >= L{minLevel}
    @Query(value = "SELECT t.member_id FROM (SELECT mr.member_id, mr.area_id, mr.rating_level FROM memberrating mr WHERE (mr.rating_level LIKE 'L%') AND CAST(SUBSTRING(mr.rating_level,2) AS UNSIGNED) >= :minLevel GROUP BY mr.member_id, mr.area_id, mr.rating_level) t GROUP BY t.member_id HAVING COUNT(DISTINCT t.area_id) >= :minAreas", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsWithMinAreasAtOrAboveLevel(int minLevel, int minAreas);

    // Return member_ids who have any rating entry with rating_level >= L{level}
    @Query(value = "SELECT DISTINCT mr.member_id FROM memberrating mr WHERE (mr.rating_level LIKE 'L%') AND CAST(SUBSTRING(mr.rating_level,2) AS UNSIGNED) >= :level", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsWithAnyAreaAtOrAboveLevel(int level);

    /**
     * 查询评级分布：按 rating_level 分组统计每个等级的成员数（去重）
     * 返回 [rating_level, count] 的列表
     * 注意：同一成员可能在多个领域有评级，这里按成员数去重
     */
    @Query(value = "SELECT mr.rating_level, COUNT(DISTINCT mr.member_id) as level_count " +
                   "FROM memberrating mr " +
                   "WHERE mr.rating_level IS NOT NULL " +
                   "GROUP BY mr.rating_level " +
                   "ORDER BY mr.rating_level ASC", nativeQuery = true)
    java.util.List<Object[]> getRatingDistribution();
}
