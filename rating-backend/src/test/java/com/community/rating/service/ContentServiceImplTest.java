package com.community.rating.service;

import com.community.rating.dto.ContentDTO;
import com.community.rating.entity.ContentSnapshot;
import com.community.rating.entity.KnowledgeArea;
import com.community.rating.entity.Member;
import com.community.rating.repository.ContentSnapshotRepository;
import com.community.rating.repository.KnowledgeAreaRepository;
import com.community.rating.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContentServiceImplTest {

    @Mock
    private ContentSnapshotRepository contentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private KnowledgeAreaRepository areaRepository;

    @InjectMocks
    private ContentServiceImpl contentService;

    private ContentSnapshot contentSnapshot1;
    private ContentSnapshot contentSnapshot2;
    private Member member;
    private KnowledgeArea knowledgeArea;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        member = new Member();
        member.setMemberId(1L);
        member.setName("测试用户");

        knowledgeArea = new KnowledgeArea();
        knowledgeArea.setAreaId(1);
        knowledgeArea.setAreaName("Java");

        // 第一个内容
        contentSnapshot1 = new ContentSnapshot();
        contentSnapshot1.setContentId(1L);
        contentSnapshot1.setMemberId(1L);
        contentSnapshot1.setAreaId(1);
        contentSnapshot1.setPublishTime(LocalDateTime.of(2023, 6, 1, 10, 0));
        contentSnapshot1.setCisScore(new BigDecimal("90"));
        contentSnapshot1.setLikeCountSnapshot(10);
        contentSnapshot1.setHateCountSnapshot(2);
        contentSnapshot1.setCommentCountSnapshot(5);
        contentSnapshot1.setShareCountSnapshot(3);
        contentSnapshot1.setReadCountSnapshot(100);
        contentSnapshot1.setCollectCountSnapshot(8);

        // 第二个内容
        contentSnapshot2 = new ContentSnapshot();
        contentSnapshot2.setContentId(2L);
        contentSnapshot2.setMemberId(1L);
        contentSnapshot2.setAreaId(1);
        contentSnapshot2.setPublishTime(LocalDateTime.of(2023, 6, 2, 10, 0));
        contentSnapshot2.setCisScore(new BigDecimal("85"));
        contentSnapshot2.setLikeCountSnapshot(8);
        contentSnapshot2.setHateCountSnapshot(1);
        contentSnapshot2.setCommentCountSnapshot(3);
        contentSnapshot2.setShareCountSnapshot(2);
        contentSnapshot2.setReadCountSnapshot(80);
        contentSnapshot2.setCollectCountSnapshot(5);
    }

    // 测试 getContentRanking 方法 - 降序排序
    @Test
    void testGetContentRanking_Descending() {
        // 准备测试数据
        Integer count = 2;
        String sortOrder = "desc";
        
        List<ContentSnapshot> contentList = Arrays.asList(contentSnapshot1, contentSnapshot2);
        Page<ContentSnapshot> contentPage = new PageImpl<>(contentList);
        
        // 设置排序和分页参数
        Sort.Direction direction = Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "cisScore");
        PageRequest pageRequest = PageRequest.of(0, count, sort);

        // 模拟依赖方法调用
        when(contentRepository.findAll(pageRequest)).thenReturn(contentPage);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(areaRepository.findById(1)).thenReturn(Optional.of(knowledgeArea));

        // 执行测试方法
        List<ContentDTO> result = contentService.getContentRanking(count, sortOrder);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // 验证排序
        assertEquals(1L, result.get(0).getContentId()); // 分数90的排第一
        assertEquals(2L, result.get(1).getContentId()); // 分数85的排第二
        
        // 验证详细信息
        assertEquals(90, result.get(0).getScore());
        assertEquals(10, result.get(0).getLikes());
        assertEquals(2, result.get(0).getUnlikes());
        assertEquals(1, result.get(0).getRank());
        
        assertEquals(85, result.get(1).getScore());
        assertEquals(2, result.get(1).getRank());

        // 验证依赖方法是否被正确调用
        verify(contentRepository).findAll(pageRequest);
        verify(memberRepository, times(2)).findById(1L); // 两个内容都调用了
        verify(areaRepository, times(2)).findById(1); // 两个内容都调用了
    }

    // 测试 getContentRanking 方法 - 升序排序
    @Test
    void testGetContentRanking_Ascending() {
        // 准备测试数据
        Integer count = 2;
        String sortOrder = "asc";
        
        List<ContentSnapshot> contentList = Arrays.asList(contentSnapshot2, contentSnapshot1);
        Page<ContentSnapshot> contentPage = new PageImpl<>(contentList);
        
        // 设置排序和分页参数
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "cisScore");
        PageRequest pageRequest = PageRequest.of(0, count, sort);

        // 模拟依赖方法调用
        when(contentRepository.findAll(pageRequest)).thenReturn(contentPage);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(areaRepository.findById(1)).thenReturn(Optional.of(knowledgeArea));

        // 执行测试方法
        List<ContentDTO> result = contentService.getContentRanking(count, sortOrder);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // 验证排序
        assertEquals(2L, result.get(0).getContentId()); // 分数85的排第一
        assertEquals(1L, result.get(1).getContentId()); // 分数90的排第二
        
        // 验证排名
        assertEquals(1, result.get(0).getRank());
        assertEquals(2, result.get(1).getRank());

        // 验证依赖方法是否被正确调用
        verify(contentRepository).findAll(pageRequest);
    }

    // 测试 getContentRanking 方法 - 默认参数
    @Test
    void testGetContentRanking_DefaultParams() {
        // 准备测试数据
        Integer count = null; // 使用默认值
        String sortOrder = null; // 使用默认值（降序）
        
        List<ContentSnapshot> contentList = Arrays.asList(contentSnapshot1);
        Page<ContentSnapshot> contentPage = new PageImpl<>(contentList);
        
        // 设置默认排序和分页参数
        Sort.Direction direction = Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "cisScore");
        PageRequest pageRequest = PageRequest.of(0, 1, sort); // 默认count为1

        // 模拟依赖方法调用
        when(contentRepository.findAll(pageRequest)).thenReturn(contentPage);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(areaRepository.findById(1)).thenReturn(Optional.of(knowledgeArea));

        // 执行测试方法
        List<ContentDTO> result = contentService.getContentRanking(count, sortOrder);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getContentId());
        assertEquals(1, result.get(0).getRank());

        // 验证依赖方法是否被正确调用
        verify(contentRepository).findAll(pageRequest);
    }

    // 测试 getContentById 方法 - 内容存在
    @Test
    void testGetContentById_ContentExists() {
        // 准备测试数据
        Long contentId = 1L;
        Double targetScore = 90.0;
        Long higherCount = 2L; // 假设有2个内容分数更高
        
        // 模拟依赖方法调用
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(contentSnapshot1));
        when(contentRepository.countContentsWithHigherScore(targetScore)).thenReturn(higherCount);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(areaRepository.findById(1)).thenReturn(Optional.of(knowledgeArea));

        // 执行测试方法
        ContentDTO result = contentService.getContentById(contentId);

        // 验证结果
        assertNotNull(result);
        assertEquals(contentId, result.getContentId());
        assertEquals(3, result.getRank()); // 排名 = 分数更高的数量 + 1
        
        // 验证详细信息
        assertEquals(90, result.getScore());
        assertEquals("测试用户", result.getAuthorName());
        assertEquals("Java", result.getDomain());

        // 验证依赖方法是否被正确调用
        verify(contentRepository).findById(contentId);
        verify(contentRepository).countContentsWithHigherScore(targetScore);
        verify(memberRepository).findById(1L);
        verify(areaRepository).findById(1);
    }

    // 测试 getContentById 方法 - 内容不存在
    @Test
    void testGetContentById_ContentNotFound() {
        // 准备测试数据
        Long contentId = 999L;
        
        // 模拟依赖方法调用
        when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

        // 执行测试方法
        ContentDTO result = contentService.getContentById(contentId);

        // 验证结果
        assertNull(result);

        // 验证依赖方法是否被正确调用
        verify(contentRepository).findById(contentId);
        verify(contentRepository, never()).countContentsWithHigherScore(anyDouble());
    }

    // 测试 getContentById 方法 - contentId 为 null
    @Test
    void testGetContentById_NullId() {
        // 执行测试方法
        ContentDTO result = contentService.getContentById(null);

        // 验证结果
        assertNull(result);

        // 验证依赖方法是否被正确调用
        verify(contentRepository, never()).findById(anyLong());
    }
}