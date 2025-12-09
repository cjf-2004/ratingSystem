package com.community.rating.service;

import com.community.rating.dto.SystemOverviewDTO;
import com.community.rating.repository.*;
import com.community.rating.simulation.TimeSimulation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SystemOverviewServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberRatingRepository memberRatingRepository;

    @Mock
    private ContentSnapshotRepository contentSnapshotRepository;

    @Mock
    private AchievementStatusRepository achievementStatusRepository;

    @Mock
    private Member_MemberRating_KnowledgeArea_Repository memberRankingRepo;

    @Mock
    private AchievementStatus_AchievementDefinition_Repository achievementStatsRepo;

    @Mock
    private RatingAlgorithm ratingAlgorithm;

    @InjectMocks
    private SystemOverviewServiceImpl systemOverviewService;

    // 固定的测试时间
    private final LocalDateTime testDateTime = LocalDateTime.of(2023, 12, 25, 15, 30, 0);
    private final Long mockTotalMembers = 1000L;

    @BeforeEach
    void setUp() {
        // 初始化模拟数据 - 只设置所有测试都需要的基础数据
        when(memberRepository.countTotalMembers()).thenReturn(mockTotalMembers);
        when(contentSnapshotRepository.count()).thenReturn(5000L);
        
        // 模拟昨日新增内容数
        when(contentSnapshotRepository.countByPublishTimeBetween(
                testDateTime.minusDays(1).toLocalDate().atStartOfDay(),
                testDateTime.toLocalDate().atStartOfDay()))
                .thenReturn(100L);
        
        // 模拟今日新增成就数
        when(achievementStatusRepository.countByAchievedTimeBetween(
                testDateTime.toLocalDate().atStartOfDay(),
                testDateTime.toLocalDate().plusDays(1).atStartOfDay()))
                .thenReturn(50L);
        
        // 模拟活跃用户数
        when(contentSnapshotRepository.countDistinctMemberIdByPublishTimeAfter(
                testDateTime.minusDays(31))).thenReturn(800L);
    }

    // 测试 getSystemOverview 方法 - 正常情况
    @Test
    void testGetSystemOverview() {
        // 设置此测试所需的额外模拟数据
        // 模拟平均评分
        when(memberRatingRepository.calculateAverageDesScoreOfLatestRatings()).thenReturn(75.5);
        when(ratingAlgorithm.formatAverageRatingLevel(75.5)).thenReturn("B+");
        
        // 模拟排名数据
        List<Object[]> mockRankingData = new ArrayList<>();
        mockRankingData.add(new Object[]{1L, "用户1", "Java", "A", new BigDecimal(95)});
        mockRankingData.add(new Object[]{2L, "用户2", "Python", "A-", new BigDecimal(92)});
        when(memberRankingRepo.findTopMembersRankingData(5)).thenReturn(mockRankingData);
        
        // 模拟成就数据
        List<Object[]> mockAchievementData = new ArrayList<>();
        mockAchievementData.add(new Object[]{"achievement_key_1", "成就名称1", 200L});
        mockAchievementData.add(new Object[]{"achievement_key_2", "成就名称2", 150L});
        when(achievementStatsRepo.findTopAchievementsStats(5)).thenReturn(mockAchievementData);
        
        // 模拟评级分布数据
        List<Object[]> mockRatingDistribution = new ArrayList<>();
        mockRatingDistribution.add(new Object[]{"A", 50L});
        mockRatingDistribution.add(new Object[]{"B+", 100L});
        mockRatingDistribution.add(new Object[]{"B", 200L});
        when(memberRatingRepository.getRatingDistribution()).thenReturn(mockRatingDistribution);
        
        // 使用MockedStatic来模拟TimeSimulation
        try (MockedStatic<TimeSimulation> mockedTime = mockStatic(TimeSimulation.class)) {
            mockedTime.when(TimeSimulation::now).thenReturn(testDateTime);
            
            // 执行测试方法
            SystemOverviewDTO result = systemOverviewService.getSystemOverview();

            // 验证结果
            assertNotNull(result);
            assertEquals(1000, result.getTotalMembers());
            assertEquals(5000, result.getTotalContents());
            assertEquals(100, result.getNewContentsToday());
            assertEquals(50, result.getNewAchievementsToday());
            assertEquals(800, result.getActiveMembers());
            assertEquals("B+", result.getAverageRating());
            assertNotNull(result.getLastUpdateTime());
            
            // 验证Top列表
            assertNotNull(result.getTopMembers());
            assertEquals(2, result.getTopMembers().size());
            assertEquals("用户1", result.getTopMembers().get(0).getMemberName());
            assertEquals("Java", result.getTopMembers().get(0).getMainDomain());
            
            assertNotNull(result.getTopAchievements());
            assertEquals(2, result.getTopAchievements().size());
            assertEquals("成就名称1", result.getTopAchievements().get(0).getAchievementName());
            assertEquals(200L, result.getTopAchievements().get(0).getAchievementCount());
            
            // 验证评级分布 - 基于最新评级总数(350)而非总成员数(1000)
            assertNotNull(result.getRatingDistribution());
            assertEquals(14.3, result.getRatingDistribution().get("A"), 0.1); // 50/350 ≈ 14.3%
            assertEquals(28.6, result.getRatingDistribution().get("B+"), 0.1); // 100/350 ≈ 28.6%
            assertEquals(57.1, result.getRatingDistribution().get("B"), 0.1); // 200/350 ≈ 57.1%

            // 验证依赖方法是否被正确调用
            verify(memberRepository).countTotalMembers();
            verify(contentSnapshotRepository).count();
            verify(contentSnapshotRepository).countByPublishTimeBetween(
                    testDateTime.minusDays(1).toLocalDate().atStartOfDay(),
                    testDateTime.toLocalDate().atStartOfDay());
            verify(achievementStatusRepository).countByAchievedTimeBetween(
                    testDateTime.toLocalDate().atStartOfDay(),
                    testDateTime.toLocalDate().plusDays(1).atStartOfDay());
        }
    }

    // 测试 getSystemOverview 方法 - 无评级数据情况
    @Test
    void testGetSystemOverview_NoRatingData() {
        // 设置此测试所需的模拟数据
        when(memberRatingRepository.calculateAverageDesScoreOfLatestRatings()).thenReturn(null);
        when(memberRatingRepository.getRatingDistribution()).thenReturn(new ArrayList<>());
        when(ratingAlgorithm.formatAverageRatingLevel(null)).thenReturn("暂无数据");
        
        // 使用MockedStatic来模拟TimeSimulation
        try (MockedStatic<TimeSimulation> mockedTime = mockStatic(TimeSimulation.class)) {
            mockedTime.when(TimeSimulation::now).thenReturn(testDateTime);
            
            // 执行测试方法
            SystemOverviewDTO result = systemOverviewService.getSystemOverview();

            // 验证结果
            assertNotNull(result);
            assertEquals("暂无数据", result.getAverageRating());
            assertNotNull(result.getRatingDistribution());
            assertTrue(result.getRatingDistribution().isEmpty());
        }
    }

    // 测试 getSystemOverview 方法 - 无Top数据情况
    @Test
    void testGetSystemOverview_NoTopData() {
        // 设置此测试所需的模拟数据
        when(memberRatingRepository.calculateAverageDesScoreOfLatestRatings()).thenReturn(75.5);
        when(ratingAlgorithm.formatAverageRatingLevel(75.5)).thenReturn("B+");
        
        // 模拟评级分布数据
        List<Object[]> mockRatingDistribution = new ArrayList<>();
        mockRatingDistribution.add(new Object[]{"A", 50L});
        when(memberRatingRepository.getRatingDistribution()).thenReturn(mockRatingDistribution);
        
        // 模拟没有Top数据的情况
        when(memberRankingRepo.findTopMembersRankingData(5)).thenReturn(new ArrayList<>());
        when(achievementStatsRepo.findTopAchievementsStats(5)).thenReturn(new ArrayList<>());
        
        // 使用MockedStatic来模拟TimeSimulation
        try (MockedStatic<TimeSimulation> mockedTime = mockStatic(TimeSimulation.class)) {
            mockedTime.when(TimeSimulation::now).thenReturn(testDateTime);
            
            // 执行测试方法
            SystemOverviewDTO result = systemOverviewService.getSystemOverview();

            // 验证结果
            assertNotNull(result);
            assertNotNull(result.getTopMembers());
            assertTrue(result.getTopMembers().isEmpty());
            assertNotNull(result.getTopAchievements());
            assertTrue(result.getTopAchievements().isEmpty());
        }
    }
}