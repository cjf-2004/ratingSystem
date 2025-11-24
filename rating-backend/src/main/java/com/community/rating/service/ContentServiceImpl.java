// com.community.rating.service.ContentServiceImpl.java
package com.community.rating.service;

import com.community.rating.dto.ContentDTO;
import com.community.rating.entity.ContentSnapshot;
// 假设您的实体和Repository路径如下
import com.community.rating.entity.Member; 
import com.community.rating.entity.KnowledgeArea;
import com.community.rating.repository.ContentSnapshotRepository;
import com.community.rating.repository.MemberRepository;
import com.community.rating.repository.KnowledgeAreaRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

@Service
@Transactional(readOnly = true)
public class ContentServiceImpl implements ContentService {

    private final ContentSnapshotRepository contentRepository;
    private final MemberRepository memberRepository;
    private final KnowledgeAreaRepository areaRepository;

    // 构造函数注入依赖
    public ContentServiceImpl(ContentSnapshotRepository contentRepository, 
                              MemberRepository memberRepository, 
                              KnowledgeAreaRepository areaRepository) {
        this.contentRepository = contentRepository;
        this.memberRepository = memberRepository;
        this.areaRepository = areaRepository;
    }

    /**
     * 辅助方法：将 ContentSnapshot 实体转换为 ContentDTO，并填充关联数据
     */
    private ContentDTO convertToDTO(ContentSnapshot entity, Integer rank) {
        // 1. 获取作者名
        String authorName = memberRepository.findById(entity.getMemberId())
                .map(Member::getName)
                .orElse("未知作者");

        // 2. 获取领域名
        String domain = areaRepository.findById(entity.getAreaId())
                .map(KnowledgeArea::getAreaName)
                .orElse("未知领域");

        ContentDTO dto = new ContentDTO();
        dto.setRank(rank);
        dto.setContentId(entity.getContentId());
        dto.setAuthorId(entity.getMemberId());
        dto.setAuthorName(authorName);
        dto.setPublishTime(entity.getPublishTime().toString()); 
        dto.setDomain(domain);
        dto.setScore(entity.getCisScore().intValue()); 
        dto.setLikes(entity.getLikeCountSnapshot());
        dto.setUnlikes(entity.getHateCountSnapshot()); 
        dto.setComments(entity.getCommentCountSnapshot());
        dto.setShares(entity.getShareCountSnapshot());
        
        return dto;
    }

    /**
     * 获取内容排名列表，按分数 (cis_score) 排序
     */
    @Override
    public List<ContentDTO> getContentRanking(Integer count, String sortOrder) {
        final int finalCount = (count == null || count < 1) ? 1 : Math.min(count, 100); 
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? 
                                   Sort.Direction.ASC : 
                                   Sort.Direction.DESC;

        // 按内容分数 cisScore 排序
        Sort sort = Sort.by(direction, "cisScore");
        PageRequest pageRequest = PageRequest.of(0, finalCount, sort);

        List<ContentSnapshot> entities = contentRepository.findAll(pageRequest).getContent();

        return entities.stream()
                // 排名从 1 开始
                .map(entity -> convertToDTO(entity, entities.indexOf(entity) + 1)) 
                .collect(Collectors.toList());
    }

    /**
     * 根据内容 ID 获取单个内容详情，并计算其分数排名
     */
    @Override
    public ContentDTO getContentById(Long contentId) {
        if (contentId == null) {
            return null; 
        }

        Optional<ContentSnapshot> contentOpt = contentRepository.findById(contentId);

        if (contentOpt.isEmpty()) {
            return null;
        }
        
        ContentSnapshot entity = contentOpt.get();
        
        // --- 核心逻辑：计算排名 ---
        
        // 1. 获取目标帖子的分数 
        Double targetScore = entity.getCisScore().doubleValue(); 
        
        // 2. 查询比目标帖子分数高的帖子数量 (需要在 Repository 中实现 countContentsWithHigherScore)
        // 排名 = (分数比我高的帖子数) + 1
        Long higherCount = contentRepository.countContentsWithHigherScore(targetScore);
        
        Integer rank = higherCount.intValue() + 1;
        
        // --- 结束核心逻辑 ---

        // 转换为 DTO，并传入计算出的排名
        return convertToDTO(entity, rank);
    }
}