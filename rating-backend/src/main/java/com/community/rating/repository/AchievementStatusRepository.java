package com.community.rating.repository;

import com.community.rating.entity.AchievementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // 导入 Repository
import java.time.LocalDateTime;

@Repository 
public interface AchievementStatusRepository extends JpaRepository<AchievementStatus, Long> {

    // AchievementStatus (单表): countByAchievedTimeAfter()
    Long countByAchievedTimeAfter(LocalDateTime time);
    
    // 检查成员是否已领取某个成就
    boolean existsByMemberIdAndAchievementKey(Long memberId, String achievementKey);
    
    // 批量查询指定成就和成员列表中已存在的记录
    java.util.List<AchievementStatus> findByAchievementKeyAndMemberIdIn(String achievementKey, java.util.List<Long> memberIds);
}