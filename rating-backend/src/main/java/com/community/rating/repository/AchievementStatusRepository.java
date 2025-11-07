package com.community.rating.repository;

import com.community.rating.entity.AchievementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // 导入 Repository
import java.time.LocalDateTime;

@Repository 
public interface AchievementStatusRepository extends JpaRepository<AchievementStatus, Long> {

    // AchievementStatus (单表): countByAchievedTimeAfter()
    Long countByAchievedTimeAfter(LocalDateTime time);
}