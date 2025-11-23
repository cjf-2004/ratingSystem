package com.community.rating.service; // 注意：直接放在 service 包下

import com.community.rating.dto.SystemOverviewDTO;
import com.community.rating.dto.TopAchievementDTO;
import com.community.rating.dto.TopMemberDTO;
import com.community.rating.repository.*;
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
            // 如果 Repository 返回 null，使用当前时间
            baseTime = LocalDateTime.now(ZoneOffset.UTC);
        }
        
        String lastUpdateTimeString = baseTime.format(ISO_FORMATTER); 

        LocalDateTime startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);

        
        // --- 2. 核心统计数据获取 ---
        Long totalMembers = memberRepository.countTotalMembers();
        Long totalContents = contentSnapshotRepository.count();
        Long newContentsToday = contentSnapshotRepository.countByPublishTimeAfter(startOfDay);
        Long newAchievementsToday = achievementStatusRepository.countByAchievedTimeAfter(startOfDay);
        Long activeMembers = contentSnapshotRepository.countDistinctMemberIdByPublishTimeAfter(thirtyDaysAgo);
        Double avgDesScore = memberRatingRepository.calculateAverageDesScoreOfLatestRatings();
        String formattedAverageRating = formatAverageRating(avgDesScore);
        
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
     * @param totalMembers 总成员数
     * @return Map<等级, 百分比>，例如 {"L1": 5.2, "L2": 12.5}
     */
    private Map<String, Double> calculateRatingDistribution(Long totalMembers) {
        Map<String, Double> distribution = new LinkedHashMap<>();
        
        // 边界情况：成员为空
        if (totalMembers == null || totalMembers == 0) {
            return distribution;
        }
        
        // 从数据库查询各等级的成员数
        List<Object[]> rawData = memberRatingRepository.getRatingDistribution();
        
        for (Object[] row : rawData) {
            String ratingLevel = (String) row[0];
            Long levelCount = ((Number) row[1]).longValue();
            
            // 计算百分比（保留 1 位小数）
            double percentage = (double) levelCount / totalMembers * 100.0;
            percentage = Math.round(percentage * 10.0) / 10.0; // 保留 1 位小数
            
            distribution.put(ratingLevel, percentage);
        }
        
        return distribution;
    }

    //计算平均等级，这里的bd只是大略的除以1000，还需要再定好等级规则后重新计算
    private String formatAverageRating(Double score) {
        if (score == null) return "N/A";
        BigDecimal bd = BigDecimal.valueOf(score).divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP);
        return "L" + bd.toString();
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
}