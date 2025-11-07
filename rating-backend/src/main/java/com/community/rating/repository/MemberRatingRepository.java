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
}
