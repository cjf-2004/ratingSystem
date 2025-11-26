package com.community.rating.service; // 注意：直接放在 service 包下

import com.community.rating.dto.SystemOverviewDTO;
import com.community.rating.dto.TopAchievementDTO;
import com.community.rating.dto.TopMemberDTO;
import com.community.rating.repository.*;
import com.community.rating.simulation.TimeSimulation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor 
@Transactional(readOnly = true)
// 实现类与接口在同一个包下
public class SystemOverviewServiceImpl implements SystemOverviewService {

    // --- 注入所有 Repository ---
    private final MemberRepository memberRepository;
    private final MemberRatingRepository memberRatingRepository;
    private final ContentSnapshotRepository contentSnapshotRepository;
    private final AchievementStatusRepository achievementStatusRepository;
    private final Member_MemberRating_KnowledgeArea_Repository memberRankingRepo;
    private final AchievementStatus_AchievementDefinition_Repository achievementStatsRepo;
    private final RatingAlgorithm ratingAlgorithm;

    private static final int TOP_LIST_LIMIT = 5;
    private static final DateTimeFormatter ISO_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");


    @Override
    public SystemOverviewDTO getSystemOverview() {
        
        // --- 1. 时间基准与日期计算 ---
        LocalDate maxUpdateDate = memberRatingRepository.findMaxUpdateDate();
        LocalDateTime baseTime;
        
        // 【关键修改：将 LocalDate 转换为当日 04:00:00 的 LocalDateTime】
        if (maxUpdateDate != null) {
            // 将 LocalDate 转换为 LocalDateTime (使用该日期的 04:00:00)
            baseTime = maxUpdateDate.atTime(04, 00, 00)
                                    .atZone(ZoneOffset.UTC) // 假设您希望将其视为 UTC 时间
                                    .toLocalDateTime(); 
        } else {
            // 如果 Repository 返回 null，使用虚拟时间
            baseTime = TimeSimulation.now();
        }
        
        String lastUpdateTimeString = baseTime.format(ISO_FORMATTER); 

        // 注意：定时任务在凌晨4点执行，所以拉取的数据都是昨天的
        // 因此 "今日" 数据实际上统计的是昨天，需要时间往前推一天
        LocalDateTime startOfDay = TimeSimulation.now().minusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime thirtyDaysAgo = TimeSimulation.now().minusDays(31);

        
        // --- 2. 核心统计数据获取 ---
        Long totalMembers = memberRepository.countTotalMembers();
        Long totalContents = contentSnapshotRepository.count();
        Long newContentsToday = contentSnapshotRepository.countByPublishTimeAfter(startOfDay);
        Long newAchievementsToday = achievementStatusRepository.countByAchievedTimeAfter(TimeSimulation.now());
        Long activeMembers = contentSnapshotRepository.countDistinctMemberIdByPublishTimeAfter(thirtyDaysAgo);
        Double avgDesScore = memberRatingRepository.calculateAverageDesScoreOfLatestRatings();
        String formattedAverageRating = ratingAlgorithm.formatAverageRatingLevel(avgDesScore);
        
        // 打印 DES 分数分布用于调整等级阈值
        printDesScoreDistribution();
        
        // 计算评级分布百分比
        Map<String, Double> ratingDistribution = calculateRatingDistribution(totalMembers);


        // --- 3. 列表数据获取与处理 ---
        List<TopMemberDTO> topMembers = getTopMembers(TOP_LIST_LIMIT);
        List<TopAchievementDTO> topAchievements = getTopAchievements(TOP_LIST_LIMIT, totalMembers);

        
        // --- 4. 最终 DTO 封装 ---
        SystemOverviewDTO dto = new SystemOverviewDTO();
        dto.setLastUpdateTime(lastUpdateTimeString);
        dto.setTotalMembers(totalMembers != null ? totalMembers.intValue() : 0);
        dto.setTotalContents(totalContents != null ? totalContents.intValue() : 0);
        dto.setActiveMembers(activeMembers != null ? activeMembers.intValue() : 0);
        dto.setNewContentsToday(newContentsToday != null ? newContentsToday.intValue() : 0);
        dto.setNewAchievementsToday(newAchievementsToday != null ? newAchievementsToday.intValue() : 0);
        dto.setAverageRating(formattedAverageRating);
        dto.setRatingDistribution(ratingDistribution);
        dto.setTopMembers(topMembers);
        dto.setTopAchievements(topAchievements);

        return dto;
    }

    /**
     * 计算各评级等级的分布百分比
     * @param totalMembers 总成员数（未使用，保持接口兼容）
     * @return Map<等级, 百分比>，例如 {"L1": 5.2, "L2": 12.5}
     */
    private Map<String, Double> calculateRatingDistribution(Long totalMembers) {
        Map<String, Double> distribution = new LinkedHashMap<>();
        
        // 从数据库查询各等级的成员数（最新评级）
        List<Object[]> rawData = memberRatingRepository.getRatingDistribution();
        
        // 第一遍：计算最新评级的总数
        long totalLatestRatings = 0;
        for (Object[] row : rawData) {
            Long levelCount = ((Number) row[1]).longValue();
            totalLatestRatings += levelCount;
        }
        
        // 边界情况：没有评级数据
        if (totalLatestRatings == 0) {
            return distribution;
        }
        
        // 第二遍：根据最新评级总数计算百分比
        for (Object[] row : rawData) {
            String ratingLevel = (String) row[0];
            Long levelCount = ((Number) row[1]).longValue();
            
            // 计算百分比：基于最新评级总数而非成员数（保留 1 位小数）
            double percentage = (double) levelCount / totalLatestRatings * 100.0;
            percentage = Math.round(percentage * 10.0) / 10.0; // 保留 1 位小数
            
            distribution.put(ratingLevel, percentage);
        }
        
        return distribution;
    }

    private List<TopMemberDTO> getTopMembers(int limit) {
        List<Object[]> rawData = memberRankingRepo.findTopMembersRankingData(limit);
        // ... (数据封装逻辑不变) ...
        List<TopMemberDTO> dtos = new ArrayList<>();
        int rank = 1;

        for (Object[] row : rawData) {
            TopMemberDTO dto = new TopMemberDTO();
            dto.setRank(rank++);
            dto.setMemberId((Long) row[0]);
            dto.setMemberName((String) row[1]);
            dto.setMainDomain((String) row[2]);
            dto.setLevel((String) row[3]);
            BigDecimal desScore = (BigDecimal) row[4];
            dto.setScore(desScore.setScale(0, RoundingMode.HALF_UP).intValue());
            dtos.add(dto);
        }
        return dtos;
    }

    private List<TopAchievementDTO> getTopAchievements(int limit, Long totalMembers) {
        List<Object[]> rawData = achievementStatsRepo.findTopAchievementsStats(limit);
        // ... (数据封装逻辑不变) ...
        List<TopAchievementDTO> dtos = new ArrayList<>();
        int rank = 1;
        long denominator = totalMembers != null && totalMembers > 0 ? totalMembers : 1; 

        for (Object[] row : rawData) {
            TopAchievementDTO dto = new TopAchievementDTO();
            dto.setRank(rank++);
            dto.setAchievementKey((String) row[0]);
            dto.setAchievementName((String) row[1]);
            Long achievementCount = (Long) row[2];
            dto.setAchievementCount(achievementCount);
            double completionRate = (double) achievementCount / (double) denominator;
            dto.setCompletionRate(completionRate);
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * 打印 DES 分数分布（用于调试和阈值规划）
     * 统计每个成员的最新 DES 分数分布，帮助确定合理的等级阈值
     */
    private void printDesScoreDistribution() {
        try {
            // 查询所有成员的最新 DES 分数
            List<Object[]> allDesScores = memberRatingRepository.getAllLatestDesScores();
            
            if (allDesScores == null || allDesScores.isEmpty()) {
                System.out.println("【DES 分数分布】无评级数据");
                return;
            }
            
            System.out.println("\n================== DES 分数分布分析 ==================");
            System.out.println("总成员数（有最新评级）: " + allDesScores.size());
            
            double minScore = Double.MAX_VALUE;
            double maxScore = Double.MIN_VALUE;
            double totalScore = 0;
            java.util.List<Double> scores = new ArrayList<>();
            
            for (Object[] row : allDesScores) {
                Double desScore = ((Number) row[0]).doubleValue();
                scores.add(desScore);
                totalScore += desScore;
                minScore = Math.min(minScore, desScore);
                maxScore = Math.max(maxScore, desScore);
            }
            
            double avgScore = totalScore / scores.size();
            
            System.out.println("\n【基本统计】");
            System.out.println("  最低分: " + String.format("%.2f", minScore));
            System.out.println("  最高分: " + String.format("%.2f", maxScore));
            System.out.println("  平均分: " + String.format("%.2f", avgScore));
            
            // 计算百分位数
            java.util.Collections.sort(scores);
            int size = scores.size();
            double p25 = scores.get(size / 4);
            double p50 = scores.get(size / 2);
            double p75 = scores.get(3 * size / 4);
            double p90 = scores.get(9 * size / 10);
            double p95 = scores.get(19 * size / 20);
            double p99 = scores.get(99 * size / 100);
            
            System.out.println("\n【百分位数】");
            System.out.println("  P25: " + String.format("%.2f", p25));
            System.out.println("  P50: " + String.format("%.2f", p50));
            System.out.println("  P75: " + String.format("%.2f", p75));
            System.out.println("  P90: " + String.format("%.2f", p90));
            System.out.println("  P95: " + String.format("%.2f", p95));
            System.out.println("  P99: " + String.format("%.2f", p99));
            
            // 按 2000 分段统计分布
            int binSize = 2000;
            int maxBin = (int) Math.ceil(maxScore / binSize) + 1;
            int[] bins = new int[maxBin + 1];
            
            for (Double score : scores) {
                int binIndex = (int) (score / binSize);
                bins[binIndex]++;
            }
            
            System.out.println("\n【分数分布（每 " + binSize + " 分一段）】");
            for (int i = 0; i <= maxBin; i++) {
                int count = bins[i];
                if (count == 0) continue; // 跳过空区间
                
                double rangeStart = i * binSize;
                double rangeEnd = (i + 1) * binSize;
                double percentage = (double) count / scores.size() * 100;
                String bar = "█".repeat(Math.min(count, 50));
                
                System.out.printf("  %5.0f - %5.0f: %3d 人 (%.1f%%) %s\n", 
                    rangeStart, rangeEnd, count, percentage, bar);
            }
            
            System.out.println("======================================================\n");
            
        } catch (Exception e) {
            System.err.println("打印 DES 分数分布失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
