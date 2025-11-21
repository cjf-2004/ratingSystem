package com.community.rating.service;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.entity.AchievementStatus;
import com.community.rating.repository.AchievementStatusRepository;
import com.community.rating.util.ProgressBar;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成就检测服务：收集所有注册的 AchievementRule，并将新达成的成就写入 `achievementstatus` 表。
 * 设计目标：规则可插拔，添加新成就只需实现 `AchievementRule` 并注册为 Spring Bean。
 */
@Service
public class AchievementDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AchievementDetectionService.class);

    private final List<AchievementRule> rules;
    private final AchievementStatusRepository statusRepository;
    private final JdbcTemplate jdbcTemplate;

    public AchievementDetectionService(List<AchievementRule> rules,
                                       AchievementStatusRepository statusRepository,
                                       JdbcTemplate jdbcTemplate) {
        this.rules = rules;
        this.statusRepository = statusRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 执行一次全量成就检测并持久化新达成的成就。可由定时任务或手动触发。
     * 优化版本：使用批量查询和批量插入以提升性能。
     */
    @Transactional
    public void detectAndPersistAchievements() {
        log.info("Achievement detection started.");
        
        // 创建进度条，以规则数量为总步骤
        ProgressBar progressBar = new ProgressBar("成就检测", rules.size());
        int totalAwardedCount = 0;
        long totalBatchQueryTime = 0;
        long totalBatchInsertTime = 0;
        
        for (AchievementRule rule : rules) {
            long ruleStartTime = System.currentTimeMillis();
            String key = rule.getAchievementKey();
            
            // 1. 检测符合条件的成员
            List<Long> candidateMemberIds = rule.detect();
            if (candidateMemberIds == null || candidateMemberIds.isEmpty()) {
                progressBar.step();
                continue;
            }
            
            // 2. 批量查询已存在的成就记录
            long queryStartTime = System.currentTimeMillis();
            List<AchievementStatus> existingStatuses = statusRepository.findByAchievementKeyAndMemberIdIn(key, candidateMemberIds);
            Set<Long> existingMemberIds = existingStatuses.stream()
                .map(AchievementStatus::getMemberId)
                .collect(Collectors.toSet());
            long queryTime = System.currentTimeMillis() - queryStartTime;
            totalBatchQueryTime += queryTime;
            
            // 3. 过滤出需要新增的成员ID
            List<Long> newMemberIds = candidateMemberIds.stream()
                .filter(id -> id != null && !existingMemberIds.contains(id))
                .collect(Collectors.toList());
            
            if (newMemberIds.isEmpty()) {
                log.debug("规则 {} 无新成就需要颁发 (候选: {}, 已存在: {})", key, candidateMemberIds.size(), existingMemberIds.size());
                progressBar.step();
                continue;
            }
            
            // 4. 批量插入新成就 (使用原生 SQL 以获得最佳性能)
            long insertStartTime = System.currentTimeMillis();
            String sql = "INSERT INTO achievementstatus (member_id, achievement_key, achieved_time) VALUES (?, ?, ?)";
            LocalDateTime now = LocalDateTime.now();
            
            jdbcTemplate.batchUpdate(sql, newMemberIds, newMemberIds.size(), 
                (ps, memberId) -> {
                    ps.setLong(1, memberId);
                    ps.setString(2, key);
                    ps.setObject(3, now);
                });
            
            long insertTime = System.currentTimeMillis() - insertStartTime;
            totalBatchInsertTime += insertTime;
            totalAwardedCount += newMemberIds.size();
            
            long ruleTime = System.currentTimeMillis() - ruleStartTime;
            log.debug("规则 {} 完成: 候选 {}, 新颁发 {}, 耗时 {} ms (查询: {} ms, 插入: {} ms)", 
                key, candidateMemberIds.size(), newMemberIds.size(), ruleTime, queryTime, insertTime);
            
            progressBar.step(); // 每处理一个规则步进一次
        }
        
        progressBar.complete(); // 完成进度条
        log.info("Achievement detection finished. Awarded {} achievements.", totalAwardedCount);
        log.info("  - 批量查询总耗时: {} ms", totalBatchQueryTime);
        log.info("  - 批量插入总耗时: {} ms", totalBatchInsertTime);
    }
}