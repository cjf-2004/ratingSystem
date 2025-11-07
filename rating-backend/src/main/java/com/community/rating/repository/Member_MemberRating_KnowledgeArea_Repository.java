// File: MemberMemberRatingKnowledgeAreaRepository.java
package com.community.rating.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository; // 导入 Repository
import java.util.List;

@Repository
public interface Member_MemberRating_KnowledgeArea_Repository extends org.springframework.data.repository.Repository<Object, Long> {

    // 【关键修改】修改 JOIN 条件中的字段名：knowledgearea -> area_id
    @Query(value = "SELECT " +
                   "    m.member_id, " +
                   "    m.name, " +
                   "    ka.area_name, " +
                   "    latest_rating.rating_level, " +
                   "    latest_rating.des_score " +
                   "FROM member m " +
                   "JOIN ( " +
                   "    SELECT mr.*, " +
                   "           ROW_NUMBER() OVER(PARTITION BY mr.member_id ORDER BY mr.update_date DESC) as rn " +
                   "    FROM memberrating mr " +
                   ") latest_rating ON m.member_id = latest_rating.member_id " +
                   "JOIN knowledgearea ka ON latest_rating.area_id = ka.area_id " + // <--- **这里是关键修改**
                   "WHERE latest_rating.rn = 1 " +
                   "ORDER BY latest_rating.des_score DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopMembersRankingData(int limit);
}