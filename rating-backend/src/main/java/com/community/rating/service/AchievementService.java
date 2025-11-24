// com.community.rating.service.AchievementService.java
package com.community.rating.service;

import com.community.rating.dto.AchievementDTO;
import java.util.List;

public interface AchievementService {
    List<AchievementDTO> getAchievementList();
    List<AchievementDTO> getAchievementRanking(Integer count, String sortOrder);
}