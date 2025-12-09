package com.community.rating.service;

import com.community.rating.dto.MemberDTO;
import com.community.rating.dto.MemberScoreHistoryDTO;
import com.community.rating.entity.KnowledgeArea;
import com.community.rating.entity.Member;
import com.community.rating.entity.MemberRating;
import com.community.rating.repository.KnowledgeAreaRepository;
import com.community.rating.repository.MemberRatingRepository;
import com.community.rating.repository.MemberRepository;
import com.community.rating.repository.Member_MemberRating_KnowledgeArea_Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberRatingRepository memberRatingRepository;

    @Mock
    private KnowledgeAreaRepository knowledgeAreaRepository;

    @Mock
    private Member_MemberRating_KnowledgeArea_Repository combinedRepo;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member member1;
    private MemberRating rating1;
    private MemberRating rating2;
    private MemberRating rating3;
    private MemberRating rating4;
    private KnowledgeArea knowledgeAreaJava;
    private KnowledgeArea knowledgeAreaPython;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        member1 = new Member();
        member1.setMemberId(1L);
        member1.setName("测试用户1");
        member1.setJoinDate(LocalDateTime.of(2023, 1, 1, 10, 0));

        knowledgeAreaJava = new KnowledgeArea();
        knowledgeAreaJava.setAreaId(1);
        knowledgeAreaJava.setAreaName("Java");
        knowledgeAreaJava.setSubTagsList("[\"核心语法\",\"集合框架\"]");

        knowledgeAreaPython = new KnowledgeArea();
        knowledgeAreaPython.setAreaId(2);
        knowledgeAreaPython.setAreaName("Python");
        knowledgeAreaPython.setSubTagsList("[\"数据科学\",\"Web开发\"]");

        rating1 = new MemberRating();
        rating1.setRatingId(1L);
        rating1.setMemberId(1L);
        rating1.setAreaId(1);
        rating1.setDesScore(new BigDecimal(90));
        rating1.setRatingLevel("A");
        rating1.setUpdateDate(LocalDate.of(2023, 6, 1));

        rating2 = new MemberRating();
        rating2.setRatingId(2L);
        rating2.setMemberId(1L);
        rating2.setAreaId(1);
        rating2.setDesScore(new BigDecimal(85));
        rating2.setRatingLevel("B");
        rating2.setUpdateDate(LocalDate.of(2023, 5, 1));

        rating3 = new MemberRating();
        rating3.setRatingId(3L);
        rating3.setMemberId(1L);
        rating3.setAreaId(2);
        rating3.setDesScore(new BigDecimal(88));
        rating3.setRatingLevel("A-");
        rating3.setUpdateDate(LocalDate.of(2023, 6, 1));
        
        rating4 = new MemberRating();
        rating4.setRatingId(4L);
        rating4.setMemberId(2L);
        rating4.setAreaId(2);
        rating4.setDesScore(new BigDecimal(80));
        rating4.setRatingLevel("B+");
        rating4.setUpdateDate(LocalDate.of(2023, 6, 1));
    }

    // 测试 getMemberRankingList 方法 - 全局排名
    @Test
    void testGetMemberRankingList_Global() {
        // 准备测试数据
        Integer limit = 5;
        Object[] row = {1L, "测试用户1", "Java", "A", new BigDecimal(90)};
        List<Object[]> rows = Collections.singletonList(row);

        // 模拟依赖方法调用
        when(combinedRepo.findTopMembersRankingData(limit)).thenReturn(rows);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member1));

        // 执行测试方法
        List<MemberDTO> result = memberService.getMemberRankingList(limit, null, null);

        // 验证结果
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getMember_id());
        assertEquals("测试用户1", result.get(0).getMember_name());
        assertEquals("Java", result.get(0).getMain_domain());
        assertEquals("A", result.get(0).getLevel());
        assertEquals(90, result.get(0).getScore());
        assertEquals(1, result.get(0).getRank());
        assertNotNull(result.get(0).getJoin_time());

        // 验证依赖方法是否被正确调用
        verify(combinedRepo).findTopMembersRankingData(limit);
        verify(memberRepository).findById(1L);
    }

    // 测试 getMemberRankingList 方法 - 指定领域排名
    @Test
    void testGetMemberRankingList_Domain() {
        // 准备测试数据
        Integer limit = 5;
        String domain = "Java";

        // 模拟依赖方法调用
        when(knowledgeAreaRepository.findByAreaName(domain)).thenReturn(Optional.of(knowledgeAreaJava));
        when(memberRatingRepository.findByAreaIdOrderByDesScoreDesc(1)).thenReturn(Arrays.asList(rating1, rating2));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member1));

        // 执行测试方法
        List<MemberDTO> result = memberService.getMemberRankingList(limit, domain, null);

        // 验证结果
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getMember_id());
        assertEquals("测试用户1", result.get(0).getMember_name());
        assertEquals("A", result.get(0).getLevel());
        assertEquals(90, result.get(0).getScore());

        // 验证依赖方法是否被正确调用
        verify(knowledgeAreaRepository).findByAreaName(domain);
        verify(memberRatingRepository).findByAreaIdOrderByDesScoreDesc(1);
    }

    // 测试 getMemberRankingList 方法 - 领域不存在
    @Test
    void testGetMemberRankingList_DomainNotFound() {
        // 准备测试数据
        Integer limit = 5;
        String domain = "不存在的领域";

        // 模拟依赖方法调用
        when(knowledgeAreaRepository.findByAreaName(domain)).thenReturn(Optional.empty());

        // 执行测试方法
        List<MemberDTO> result = memberService.getMemberRankingList(limit, domain, null);

        // 验证结果
        assertTrue(result.isEmpty());

        // 验证依赖方法是否被正确调用
        verify(knowledgeAreaRepository).findByAreaName(domain);
        verify(memberRatingRepository, never()).findByAreaIdOrderByDesScoreDesc(anyInt());
    }

    // 测试 getMemberRankingList 方法 - 升序排序
    @Test
    void testGetMemberRankingList_Asc() {
        // 准备测试数据
        Integer limit = 5;
        String sortBy = "asc";

        Object[] row1 = {1L, "测试用户1", "Java", "A", new BigDecimal(90)};
        Object[] row2 = {2L, "测试用户2", "Python", "B", new BigDecimal(85)};
        List<Object[]> rows = Arrays.asList(row1, row2);

        Member member2 = new Member();
        member2.setMemberId(2L);
        member2.setName("测试用户2");
        member2.setJoinDate(LocalDateTime.of(2023, 2, 1, 10, 0));

        // 模拟依赖方法调用
        when(combinedRepo.findTopMembersRankingData(limit)).thenReturn(rows);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member1));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member2));

        // 执行测试方法
        List<MemberDTO> result = memberService.getMemberRankingList(limit, null, sortBy);

        // 验证结果
        assertEquals(2, result.size());
        // 升序排序，分数低的在前
        assertEquals(2L, result.get(0).getMember_id()); // 分数85的排第一
        assertEquals(1L, result.get(1).getMember_id()); // 分数90的排第二
        assertEquals(85, result.get(0).getScore());
        assertEquals(90, result.get(1).getScore());

        // 验证依赖方法是否被正确调用
        verify(combinedRepo).findTopMembersRankingData(limit);
        verify(memberRepository).findById(1L);
        verify(memberRepository).findById(2L);
    }

    // 测试 getMember 方法 - 成员存在
    @Test
    void testGetMember_MemberExists() {
        // 准备测试数据
        Long memberId = 1L;

        // 模拟依赖方法调用
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member1));
        when(memberRatingRepository.findAllByMemberId(memberId)).thenReturn(Arrays.asList(rating1, rating2, rating3));
        when(knowledgeAreaRepository.findById(1)).thenReturn(Optional.of(knowledgeAreaJava));
        when(knowledgeAreaRepository.findById(2)).thenReturn(Optional.of(knowledgeAreaPython));
        when(memberRatingRepository.findByAreaIdOrderByDesScoreDesc(1)).thenReturn(Arrays.asList(rating1, rating2));
        when(memberRatingRepository.findByAreaIdOrderByDesScoreDesc(2)).thenReturn(Collections.singletonList(rating3));

        // 执行测试方法
        List<MemberScoreHistoryDTO> result = memberService.getMember(memberId);

        // 验证结果
        assertEquals(2, result.size()); // 该成员在两个领域有评级

        // 验证Java领域的DTO
        MemberScoreHistoryDTO javaDto = result.stream()
                .filter(dto -> "Java".equals(dto.getMain_domain()))
                .findFirst()
                .orElse(null);
        assertNotNull(javaDto);
        assertEquals(memberId, javaDto.getMember_id());
        assertEquals("测试用户1", javaDto.getMember_name());
        assertEquals("Java", javaDto.getMain_domain());
        assertEquals("A", javaDto.getLevel());
        assertEquals(90, javaDto.getScore());
        assertNotNull(javaDto.getRank());
        assertNotNull(javaDto.getScore_history());
        assertEquals(2, javaDto.getScore_history().size()); // Java领域有两条历史记录

        // 验证Python领域的DTO
        MemberScoreHistoryDTO pythonDto = result.stream()
                .filter(dto -> "Python".equals(dto.getMain_domain()))
                .findFirst()
                .orElse(null);
        assertNotNull(pythonDto);
        assertEquals("Python", pythonDto.getMain_domain());
        assertEquals("A-", pythonDto.getLevel());
        assertEquals(88, pythonDto.getScore());
        assertNotNull(pythonDto.getRank());

        // 验证依赖方法是否被正确调用
        verify(memberRepository).findById(memberId);
        verify(memberRatingRepository).findAllByMemberId(memberId);
        verify(knowledgeAreaRepository).findById(1);
        verify(knowledgeAreaRepository).findById(2);
    }

    // 测试 getMember 方法 - 成员不存在
    @Test
    void testGetMember_MemberNotExists() {
        // 准备测试数据
        Long memberId = 999L;

        // 模拟依赖方法调用
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // 执行测试方法
        List<MemberScoreHistoryDTO> result = memberService.getMember(memberId);

        // 验证结果
        assertTrue(result.isEmpty());

        // 验证依赖方法是否被正确调用
        verify(memberRepository).findById(memberId);
        verify(memberRatingRepository, never()).findAllByMemberId(anyLong());
    }

    // 测试 getMember 方法 - 成员没有评级记录
    @Test
    void testGetMember_NoRatings() {
        // 准备测试数据
        Long memberId = 1L;

        // 模拟依赖方法调用
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member1));
        when(memberRatingRepository.findAllByMemberId(memberId)).thenReturn(new ArrayList<>());

        // 执行测试方法
        List<MemberScoreHistoryDTO> result = memberService.getMember(memberId);

        // 验证结果
        assertEquals(1, result.size());
        MemberScoreHistoryDTO dto = result.get(0);
        assertEquals(memberId, dto.getMember_id());
        assertEquals("测试用户1", dto.getMember_name());
        assertNull(dto.getMain_domain());
        assertNull(dto.getLevel());
        assertNull(dto.getScore());
        assertNull(dto.getRank());
        assertNotNull(dto.getJoin_time());
        assertNotNull(dto.getScore_history());
        assertTrue(dto.getScore_history().isEmpty());

        // 验证依赖方法是否被正确调用
        verify(memberRepository).findById(memberId);
        verify(memberRatingRepository).findAllByMemberId(memberId);
        verify(knowledgeAreaRepository, never()).findById(anyInt());
    }

    // 测试 searchMembers 方法 - 关键词匹配
    @Test
    void testSearchMembers_KeywordMatch() {
        // 准备测试数据
        String keyword = "测试";
        Integer limit = 5;

        // 模拟依赖方法调用
        when(memberRepository.findByNameContainingIgnoreCase(keyword)).thenReturn(Arrays.asList(member1));
        when(memberRatingRepository.findAllByMemberId(1L)).thenReturn(Arrays.asList(rating1));
        when(knowledgeAreaRepository.findById(1)).thenReturn(Optional.of(knowledgeAreaJava));

        // 执行测试方法
        List<MemberDTO> result = memberService.searchMembers(keyword, null, limit);

        // 验证结果
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getMember_id());
        assertEquals("测试用户1", result.get(0).getMember_name());

        // 验证依赖方法是否被正确调用
        verify(memberRepository).findByNameContainingIgnoreCase(keyword);
        verify(memberRatingRepository).findAllByMemberId(1L);
    }

    // 测试 searchMembers 方法 - 关键词不匹配
    @Test
    void testSearchMembers_KeywordNoMatch() {
        // 准备测试数据
        String keyword = "不存在的关键词";
        Integer limit = 5;

        // 模拟依赖方法调用
        when(memberRepository.findByNameContainingIgnoreCase(keyword)).thenReturn(new ArrayList<>());

        // 执行测试方法
        List<MemberDTO> result = memberService.searchMembers(keyword, null, limit);

        // 验证结果
        assertTrue(result.isEmpty());

        // 验证依赖方法是否被正确调用
        verify(memberRepository).findByNameContainingIgnoreCase(keyword);
        verify(memberRatingRepository, never()).findAllByMemberId(anyLong());
    }

    // 测试 searchMembers 方法 - 关键词匹配+指定领域
    @Test
    void testSearchMembers_KeywordMatchWithDomain() {
        // 准备测试数据
        String keyword = "测试";
        String domain = "Java";
        Integer limit = 5;

        // 创建另一个领域的成员
        Member member2 = new Member();
        member2.setMemberId(2L);
        member2.setName("另一个测试用户");
        member2.setJoinDate(LocalDateTime.of(2023, 3, 1, 10, 0));

        // 模拟依赖方法调用
        when(memberRepository.findByNameContainingIgnoreCase(keyword)).thenReturn(Arrays.asList(member1, member2));
        when(knowledgeAreaRepository.findByAreaName(domain)).thenReturn(Optional.of(knowledgeAreaJava));
        
        // member1在Java领域有评级
        when(memberRatingRepository.findByMemberId(1L)).thenReturn(Arrays.asList(rating1));
        when(memberRatingRepository.findByMemberId(2L)).thenReturn(Arrays.asList(rating4));

        // 执行测试方法
        List<MemberDTO> result = memberService.searchMembers(keyword, domain, limit);

        // 验证结果
        assertEquals(1, result.size()); // 只有member1在Java领域有评级
        assertEquals(1L, result.get(0).getMember_id());
        assertEquals("测试用户1", result.get(0).getMember_name());

        // 验证依赖方法是否被正确调用
        verify(memberRepository).findByNameContainingIgnoreCase(keyword);
        verify(knowledgeAreaRepository).findByAreaName(domain);
    }
}