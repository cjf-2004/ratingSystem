package com.community.rating.service;

import com.community.rating.simulation.ForumDataSimulation;
import com.community.rating.dto.ContentDataDTO;
import com.community.rating.entity.ContentSnapshot;
import com.community.rating.entity.Member;
import com.community.rating.entity.MemberRating;
import com.community.rating.entity.KnowledgeArea;
import com.community.rating.repository.ContentSnapshotRepository;
import com.community.rating.repository.MemberRatingRepository;
import com.community.rating.repository.KnowledgeAreaRepository;
import com.community.rating.repository.MemberRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 【RatingCalculationService】
 * 职责：定时任务调度、数据拉取、结果存储的协调者。
 * 已重构，适配表结构：使用 areaId (Integer) 替代 knowledgeTag (String)。
 * 新增：领域标签到 ID 的解析和缓存逻辑。
 */
@Service
public class RatingCalculationService {

    private static final Logger log = LoggerFactory.getLogger(RatingCalculationService.class);
    private final ForumDataSimulation forumDataSimulation;
    private final RatingAlgorithm ratingAlgorithm = new RatingAlgorithm();
    
    private final ContentSnapshotRepository contentSnapshotRepository;
    private final MemberRatingRepository memberRatingRepository;
    private final KnowledgeAreaRepository knowledgeAreaRepository;
    private final MemberRepository memberRepository; // 新增注入 MemberRepository
    
    // 缓存：用于存储 knowledgeTag -> areaId 的映射，避免重复查询数据库
    private final Map<String, Integer> tagToIdCache = new ConcurrentHashMap<>();

    public RatingCalculationService(
        ForumDataSimulation forumDataSimulation, 
        ContentSnapshotRepository contentSnapshotRepository,
        MemberRatingRepository memberRatingRepository,
        KnowledgeAreaRepository knowledgeAreaRepository,
        MemberRepository memberRepository) // 新增构造参数
    {
        this.forumDataSimulation = forumDataSimulation;
        this.contentSnapshotRepository = contentSnapshotRepository;
        this.memberRatingRepository = memberRatingRepository;
        this.knowledgeAreaRepository = knowledgeAreaRepository;
        this.memberRepository = memberRepository; // 新增赋值
    }
    
    /**
     * 辅助方法：初始化标签到 ID 的映射缓存。
     * 必须在执行计算前调用。
     */
    private void initializeAreaIdCache() {
        log.info("初始化 knowledgeTag 到 areaId 的映射缓存...");
        // knowledgeArea 的 areaName 对应于 simulation 返回的 knowledge_tag
        List<KnowledgeArea> areas = knowledgeAreaRepository.findAll();
        tagToIdCache.clear();
        areas.forEach(area -> tagToIdCache.put(area.getAreaName(), area.getAreaId()));
        log.info("缓存初始化完成，共加载 {} 个领域。", tagToIdCache.size());
        
        if (tagToIdCache.isEmpty()) {
            log.error("警告：知识领域表为空！无法进行标签到 ID 的转换，DES/CIS 评分可能无法正常进行。");
        }
    }

    /**
     * 接口：每日/定时执行全量评级计算。
     */
    @Scheduled(fixedRate = 30000)
    @Transactional 
    public void executeDailyRatingCalculation() {
        // 1. 确保标签映射是最新
        initializeAreaIdCache();
        
        // 新增：同步成员数据，防止外键约束问题
        log.info("--- 0. 开始执行【成员数据同步】任务 ---");
        syncMemberDataFromSnapshot();
        
        log.info("--- 1. 开始执行【内容影响力分数 (CIS)】计算任务 ---");
        
        List<ContentDataDTO> contentDTOsWithCIS = calculateAllContentCIS();

        log.info("--- 2. 开始执行【成员领域专精度得分 (DES)】计算任务 ---");
        
        updateAllMemberRankings(contentDTOsWithCIS);
        
        log.info("--- 评级定时计算任务执行完毕。---");
    }

    /**
     * 新增方法：从论坛快照中同步成员数据。
     * 使用 REQUIRES_NEW 确保该方法在独立事务中运行，并在完成后立即提交，
     * 即使外部调用方 (executeDailyRatingCalculation) 没有事务或仍在事务中。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void syncMemberDataFromSnapshot() {
        
        try {
            // 获取成员快照数据
            List<Map<String, Object>> memberSnapshotMaps = forumDataSimulation.getMemberSnapshot();
            
            if (memberSnapshotMaps.isEmpty()) {
                log.warn("未拉取到任何成员快照数据，跳过成员同步。");
                return;
            }
            
            int newMemberCount = 0;
            
            // 遍历所有成员快照，检查并添加新成员
            for (Map<String, Object> memberMap : memberSnapshotMaps) {
                Long memberId = safeToLong(memberMap, "member_id");
                
                // 跳过无效的成员ID
                if (memberId == null) {
                    continue;
                }
                
                // 检查成员是否已存在
                Optional<Member> existingMember = memberRepository.findById(memberId);
                
                if (!existingMember.isPresent()) {
                    // 创建新成员
                    Member newMember = new Member();
                    newMember.setMemberId(memberId);
                    newMember.setName((String) memberMap.get("name"));
                    
                    // 解析加入日期
                    String joinDateStr = (String) memberMap.get("join_date");
                    if (joinDateStr != null) {
                        newMember.setJoinDate(LocalDateTime.parse(joinDateStr));
                    } else {
                        // 如果没有提供加入日期，使用当前时间
                        newMember.setJoinDate(LocalDateTime.now());
                    }
                    
                    // 保存新成员
                    memberRepository.save(newMember);
                    newMemberCount++;
                    log.debug("新增成员: ID={}, 名称={}", memberId, newMember.getName());
                }
            }
            
            log.info("成员数据同步完成，新增 {} 个成员。", newMemberCount);
        } catch (Exception e) {
            log.error("成员数据同步失败: {}", e.getMessage(), e);
        }
        
        log.info("--- 成员数据同步任务执行完毕。---");
    }

    /**
     * 职责：计算所有内容的 CIS，并持久化到 ContentSnapshotRepository。
     */
    @Transactional
    private List<ContentDataDTO> calculateAllContentCIS() {
        List<Map<String, Object>> snapshotMaps = forumDataSimulation.getContentSnapshot();
        if (snapshotMaps.isEmpty()) {
            log.warn("未拉取到任何内容快照，跳过 CIS 计算。");
            return List.of();
        }

        List<ContentDataDTO> calculatedContentDTOs = snapshotMaps.stream()
            .map(this::mapToDTO) // 包含标签到 ID 的转换
            .filter(dto -> dto.getAreaId() != null) // 过滤掉无法解析 areaId 的内容
            .peek(dto -> {
                BigDecimal cisScore = ratingAlgorithm.calculateCIS(dto);
                dto.setCisScore(cisScore); 
                
                // 持久化 CIS 结果到 ContentSnapshot
                saveCISToDatabase(dto);
            })
            .collect(Collectors.toList());

        return calculatedContentDTOs;
    }

    /**
     * 持久化单个 DTO 的 CIS 结果到数据库。
     */
    private void saveCISToDatabase(ContentDataDTO dto) {
        ContentSnapshot entity = new ContentSnapshot();
        
        // 映射核心字段
        entity.setContentId(dto.getContentId());
        entity.setMemberId(dto.getMemberId());
        entity.setAreaId(dto.getAreaId()); // 使用 areaId
        entity.setPublishTime(dto.getPublishTime());
        entity.setPostLengthLevel(dto.getPostLengthLevel());
        
        // 核心转换：Long (DTO) -> Integer (Entity)，并安全处理 null
        entity.setReadCountSnapshot(Optional.ofNullable(dto.getReadCount()).orElse(0L).intValue());
        entity.setLikeCountSnapshot(Optional.ofNullable(dto.getLikeCount()).orElse(0L).intValue());
        entity.setCommentCountSnapshot(Optional.ofNullable(dto.getCommentCount()).orElse(0L).intValue());
        entity.setShareCountSnapshot(Optional.ofNullable(dto.getShareCount()).orElse(0L).intValue());
        entity.setCollectCountSnapshot(Optional.ofNullable(dto.getCollectCount()).orElse(0L).intValue());
        entity.setHateCountSnapshot(Optional.ofNullable(dto.getHateCount()).orElse(0L).intValue());
        
        // 结果
        entity.setCisScore(dto.getCisScore());
        
        contentSnapshotRepository.save(entity);
        log.debug("保存 CIS 结果到 DB. ContentID: {}, CIS: {}", dto.getContentId(), dto.getCisScore().toPlainString());
    }

    /**
     * 职责：基于已计算的 CIS DTOs 计算 DES 并持久化到 MemberRatingRepository。
     */
    @Transactional
    private void updateAllMemberRankings(List<ContentDataDTO> contentDTOsWithCIS) {
        if (contentDTOsWithCIS.isEmpty()) {
            log.warn("没有 CIS 数据，跳过 DES 计算。");
            return;
        }
        
        // 1. 按 (memberId, areaId) 对内容进行分组 (使用 Integer areaId)
        Map<Long, Map<Integer, List<ContentDataDTO>>> memberContentGroup = contentDTOsWithCIS.stream()
            .collect(Collectors.groupingBy(
                ContentDataDTO::getMemberId,
                Collectors.groupingBy(ContentDataDTO::getAreaId)
            ));

        // 2. 遍历每个成员和其在不同领域的内容，计算 DES
        memberContentGroup.forEach((memberId, areaGroups) -> {
            areaGroups.forEach((areaId, contents) -> {
                calculateMemberDESAndSave(memberId, areaId, contents);
            });
        });
    }

    /**
     * 职责：计算特定成员在特定领域 K 的 DES 分数和评级，并持久化。
     */
    private void calculateMemberDESAndSave(Long memberId, Integer areaId, List<ContentDataDTO> contents) {
        BigDecimal desScore = ratingAlgorithm.calculateDES(contents);
        String ratingLevel = ratingAlgorithm.determineRatingLevel(desScore);

        // 查找或创建实体 (使用 memberId 和 areaId 查询)
        Optional<MemberRating> existingRating = memberRatingRepository
            .findByMemberIdAndAreaId(memberId, areaId); // 使用 findByMemberIdAndAreaId
        
        MemberRating entity = existingRating.orElseGet(MemberRating::new);
        
        // 设置所有必需字段
        entity.setMemberId(memberId);
        entity.setAreaId(areaId);
        
        // 设置新值并保存
        entity.setDesScore(desScore);
        entity.setRatingLevel(ratingLevel);
        entity.setUpdateDate(LocalDateTime.now()); 
        
        memberRatingRepository.save(entity);
        
        log.debug("【DES结果】MEMBER: {}, 领域ID: {}, DES: {}, 评级: {}", 
            memberId, areaId, desScore.toPlainString(), ratingLevel);
    }

    /**
     * 辅助方法：将 Map 结构转换为 DTO 对象，并解析 knowledgeTag 为 areaId。
     */
    private ContentDataDTO mapToDTO(Map<String, Object> map) {
        ContentDataDTO dto = new ContentDataDTO();
        try {
            dto.setContentId(safeToLong(map, "content_id"));
            dto.setMemberId(safeToLong(map, "member_id"));
            
            // 1. 获取 knowledge_tag
            String knowledgeTag = (String) map.get("knowledge_tag");
            if (knowledgeTag != null) {
                // 2. 通过缓存解析 areaId
                Integer areaId = tagToIdCache.get(knowledgeTag);
                
                // areaId 不为 null 时才设置 DTO
                if (areaId == null) {
                    log.warn("内容 ID: {} 无法解析 knowledgeTag: {} 为 areaId，该内容将被过滤。", dto.getContentId(), knowledgeTag);
                }
                dto.setAreaId(areaId); 
            }
            
            // 映射其余字段
            if (map.get("publish_time") instanceof String) {
                 dto.setPublishTime(LocalDateTime.parse((String) map.get("publish_time"))); 
            }
            
            dto.setPostLengthLevel(safeToInteger(map, "post_length_level"));

            // 计数转换为 Long
            dto.setReadCount(safeToLong(map, "read_count_snapshot"));
            dto.setLikeCount(safeToLong(map, "like_count_snapshot"));
            dto.setCommentCount(safeToLong(map, "comment_count_snapshot"));
            dto.setShareCount(safeToLong(map, "share_count_snapshot"));
            dto.setCollectCount(safeToLong(map, "collect_count_snapshot"));
            dto.setHateCount(safeToLong(map, "hate_count_snapshot"));
            
        } catch (Exception e) {
            log.error("映射 Content 快照数据到 DTO 失败: {}", map, e);
        }
        return dto;
    }
    
    // 安全转换辅助方法（保持不变）
    private Long safeToLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null; 
    }

    private Integer safeToInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null; 
    }
}