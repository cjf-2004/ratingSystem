package com.community.rating.repository;

import com.community.rating.entity.MemberRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRatingRepository extends JpaRepository<MemberRating, Long> {

    /**
     * 根据知识领域 ID 查询该领域下所有成员的评级，并按分数降序排列（用于排名榜单）
     * 对应 SQL: SELECT * FROM member_rating WHERE knowledge_area_id = ? ORDER BY des_score DESC
     */
    List<MemberRating> findByKnowledgeAreaIdOrderByDesScoreDesc(Integer areaId);

    /**
     * 根据成员 ID 查询其在所有领域的评级
     */
    List<MemberRating> findByMemberId(Long memberId);
}
