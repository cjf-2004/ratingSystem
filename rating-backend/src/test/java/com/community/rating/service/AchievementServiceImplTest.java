package com.community.rating.service;

import com.community.rating.dto.AchievementDTO;
import com.community.rating.repository.AchievementStatus_AchievementDefinition_Repository;
import com.community.rating.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AchievementServiceImplTest {

    @Mock
    private AchievementStatus_AchievementDefinition_Repository customRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AchievementServiceImpl achievementService;

    private List<Object[]> mockResults;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockResults = new ArrayList<>();
        mockResults.add(new Object[]{"achievement_key_1", "成就名称1", "类型1", "触发条件1", 100L});
        mockResults.add(new Object[]{"achievement_key_2", "成就名称2", "类型2", "触发条件2", 50L});
    }

    // 测试 getAchievementList 方法
    @Test
    void testGetAchievementList() {
        // 模拟依赖方法调用
        when(memberRepository.count()).thenReturn(1000L);
        when(customRepository.findAllAchievementsWithStats()).thenReturn(mockResults);

        // 执行测试方法
        List<AchievementDTO> result = achievementService.getAchievementList();

        // 验证结果
        assertEquals(2, result.size());
        assertEquals("achievement_key_1", result.get(0).getAchievementKey());
        assertEquals("成就名称1", result.get(0).getName());
        assertEquals("类型1", result.get(0).getCategory());
        assertEquals("触发条件1", result.get(0).getDescription());
        assertEquals(100, result.get(0).getAchievedCount());
        assertEquals(0.1, result.get(0).getCompletionRate()); // 100/1000 = 0.1
        assertNull(result.get(0).getRank());

        // 验证依赖方法是否被正确调用
        verify(memberRepository).count();
        verify(customRepository).findAllAchievementsWithStats();
    }

    // 测试 getAchievementRanking 方法 - 降序排序
    @Test
    void testGetAchievementRanking_Desc() {
        // 模拟依赖方法调用
        when(memberRepository.count()).thenReturn(1000L);
        when(customRepository.findAllAchievementsWithStats()).thenReturn(mockResults);

        // 执行测试方法
        List<AchievementDTO> result = achievementService.getAchievementRanking(10, "desc");

        // 验证结果
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRank()); // 第一个成就完成数更多，排名第一
        assertEquals(2, result.get(1).getRank());

        // 验证依赖方法是否被正确调用
        verify(memberRepository).count();
        verify(customRepository).findAllAchievementsWithStats();
    }

    // 测试 getAchievementRanking 方法 - 升序排序
    @Test
    void testGetAchievementRanking_Asc() {
        // 模拟依赖方法调用
        when(memberRepository.count()).thenReturn(1000L);
        when(customRepository.findAllAchievementsWithStats()).thenReturn(mockResults);

        // 执行测试方法
        List<AchievementDTO> result = achievementService.getAchievementRanking(10, "asc");

        // 验证结果
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRank()); // 升序排序时，第一个元素（完成数50）排名1
        assertEquals(2, result.get(1).getRank()); // 第二个元素（完成数100）排名2

        // 验证依赖方法是否被正确调用
        verify(memberRepository).count();
        verify(customRepository).findAllAchievementsWithStats();
    }

    // 测试总成员数为0的情况
    @Test
    void testGetAchievementList_ZeroMembers() {
        // 模拟依赖方法调用
        when(memberRepository.count()).thenReturn(0L);
        when(customRepository.findAllAchievementsWithStats()).thenReturn(mockResults);

        // 执行测试方法
        List<AchievementDTO> result = achievementService.getAchievementList();

        // 验证结果
        assertEquals(2, result.size());
        assertEquals(100.0, result.get(0).getCompletionRate()); // 100/1 = 100.0，因为achievedCount是100，finalTotalMembers是1.0

        // 验证依赖方法是否被正确调用
        verify(memberRepository).count();
        verify(customRepository).findAllAchievementsWithStats();
    }
}