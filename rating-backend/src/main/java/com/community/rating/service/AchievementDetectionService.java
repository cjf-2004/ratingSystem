package com.community.rating.service;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.entity.AchievementDefinition;
import com.community.rating.entity.AchievementStatus;
import com.community.rating.repository.AchievementDefinitionRepository;
import com.community.rating.repository.AchievementStatusRepository;
import com.community.rating.util.ProgressBar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成就检测服务：收集所有注册的 AchievementRule，并将新达成的成就写入 `achievementstatus` 表。
 * 设计目标：规则可插拔，添加新成就只需实现 `AchievementRule` 并注册为 Spring Bean。
 */
@Service
public class AchievementDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AchievementDetectionService.class);

    private final List<AchievementRule> rules;
    private final AchievementStatusRepository statusRepository;
    private final AchievementDefinitionRepository definitionRepository;

    public AchievementDetectionService(List<AchievementRule> rules,
                                       AchievementStatusRepository statusRepository,
                                       AchievementDefinitionRepository definitionRepository) {
        this.rules = rules;
        this.statusRepository = statusRepository;
        this.definitionRepository = definitionRepository;
    }

    /**
     * 执行一次全量成就检测并持久化新达成的成就。可由定时任务或手动触发。
     */
    @Transactional
    public void detectAndPersistAchievements() {
        log.info("Achievement detection started.");
        
        // 创建进度条，以规则数量为总步骤
        ProgressBar progressBar = new ProgressBar("成就检测", rules.size());
        int awardedCount = 0;
        
        for (AchievementRule rule : rules) {
            String key = rule.getAchievementKey();
            List<Long> memberIds = rule.detect();
            if (memberIds == null || memberIds.isEmpty()) {
                progressBar.step();
                continue;
            }

            for (Long memberId : memberIds) {
                if (memberId == null) continue;
                boolean exists = statusRepository.existsByMemberIdAndAchievementKey(memberId, key);
                if (exists) continue; // 已发放，跳过

                AchievementStatus s = new AchievementStatus();
                s.setMemberId(memberId);
                s.setAchievementKey(key);
                s.setAchievedTime(LocalDateTime.now());
                // Ensure the achievement definition exists to satisfy FK constraint.
                // if (!definitionRepository.existsById(key)) {
                //     AchievementDefinition def = new AchievementDefinition();
                //     def.setAchievementKey(key);
                //     def.setName(key);
                //     def.setType("auto");
                //     def.setTriggerConditionDesc("Automatically created by AchievementDetectionService");
                //     definitionRepository.save(def);
                // }

                statusRepository.save(s);
                awardedCount++;
            }
            
            progressBar.step(); // 每处理一个规则步进一次
        }
        
        progressBar.complete(); // 完成进度条
        log.info("Achievement detection finished. Awarded {} achievements.", awardedCount);
    }
}