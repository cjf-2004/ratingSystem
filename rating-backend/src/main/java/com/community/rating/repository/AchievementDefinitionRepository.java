package com.community.rating.repository;

import com.community.rating.entity.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, String> {
}
