package com.community.rating.service;

// 保留原有的imports，添加ProgressBar的import
import com.community.rating.util.ProgressBar;

import com.community.rating.simulation.ForumDataSimulation;
import com.community.rating.simulation.TimeSimulation;
import com.community.rating.dto.ContentDataDTO;
import com.community.rating.entity.ContentSnapshot;
import com.community.rating.entity.Member;
import com.community.rating.entity.MemberRating;
import com.community.rating.entity.KnowledgeArea;
import com.community.rating.repository.KnowledgeAreaRepository;
import com.community.rating.repository.MemberRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 【RatingCalculationService】
 * 职责：定时任务调度、数据拉取、结果存储的协调者。
 * 已重构，适配表结构：使用 areaId (Integer) 替代 knowledgeTag (String)。
 * 新增：领域标签到 ID 的解析和缓存逻辑。
 * 
 * 【虚拟时间定时】每天虚拟时间凌晨 4 点执行一次，而非固定间隔
 */
@Service
public class RatingCalculationService {

    private static final Logger log = LoggerFactory.getLogger(RatingCalculationService.class);
    private final ForumDataSimulation forumDataSimulation;
    private final RatingAlgorithm ratingAlgorithm;
    
    private final KnowledgeAreaRepository knowledgeAreaRepository;
    private final MemberRepository memberRepository; // 新增注入 MemberRepository
    private final AchievementDetectionService achievementDetectionService;
    private final JdbcTemplate jdbcTemplate;
    
    // 缓存：用于存储 knowledgeTag -> areaId 的映射，避免重复查询数据库
    private final Map<String, Integer> tagToIdCache = new ConcurrentHashMap<>();
    
    // 【虚拟时间定时】记录上一次执行的虚拟日期，用于检测是否跨越到新的一天凌晨 4 点
    private LocalDate lastExecutionDate = null;

    public RatingCalculationService(
        ForumDataSimulation forumDataSimulation, 
        RatingAlgorithm ratingAlgorithm,
        KnowledgeAreaRepository knowledgeAreaRepository,
        MemberRepository memberRepository,
        AchievementDetectionService achievementDetectionService,
        JdbcTemplate jdbcTemplate) // 新增构造参数
    {
        this.forumDataSimulation = forumDataSimulation;
        this.ratingAlgorithm = ratingAlgorithm; // 新增赋值
        this.knowledgeAreaRepository = knowledgeAreaRepository;
        this.memberRepository = memberRepository; // 新增赋值
        this.achievementDetectionService = achievementDetectionService;
        this.jdbcTemplate = jdbcTemplate;
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
     * 虚拟时间定时触发器：每隔 5 秒检查一次虚拟时间，当达到凌晨 4 点时执行评级计算
     * 这样可以适配虚拟时间加速的场景（288x 加速）
     */
    @Scheduled(fixedRate = 2000) // 真实时间每 2 秒检查一次
    public void checkAndExecuteVirtualTimeTask() {
        LocalDateTime now = TimeSimulation.now();
        LocalDate currentDate = now.toLocalDate();
        int currentHour = now.getHour();
        
        // 检查虚拟时间是否在凌晨 4 点，且这是新的一天
        if (currentHour == 4 && !currentDate.equals(lastExecutionDate)) {
            log.info("【虚拟时间定时触发】检测到虚拟时间凌晨 4 点: {}", now);
            lastExecutionDate = currentDate;
            executeDailyRatingCalculation();
        }
    }
    
    /**
     * 核心评级计算逻辑（由虚拟时间触发器调用）
     */
    @Transactional 
    public void executeDailyRatingCalculation() {
        // 【打印虚拟时间】
        LocalDateTime virtualNow = TimeSimulation.now();
        log.info("【虚拟时间定时执行】开始执行定时任务，虚拟时间: {}", virtualNow);
        
        long totalStartTime = System.currentTimeMillis();
        Map<String, Long> timingStats = new ConcurrentHashMap<>();
        
        // 0. 初始化缓存
        long cacheStartTime = System.currentTimeMillis();
        initializeAreaIdCache();
        timingStats.put("0. 缓存初始化", System.currentTimeMillis() - cacheStartTime);
        
        // 1. 成员数据同步
        log.info("--- 0. 开始执行【成员数据同步】任务 ---");
        long memberSyncStartTime = System.currentTimeMillis();
        syncMemberDataFromSnapshot();
        timingStats.put("1. 成员数据同步", System.currentTimeMillis() - memberSyncStartTime);
        
        // 2. CIS 计算
        log.info("--- 1. 开始执行【内容影响力分数 (CIS)】计算任务 ---");
        long cisStartTime = System.currentTimeMillis();
        List<ContentDataDTO> allContentDTOsWithUpdatedCIS = calculateAllContentCIS();
        timingStats.put("2. CIS计算", System.currentTimeMillis() - cisStartTime);

        // 3. DES 计算
        log.info("--- 2. 开始执行【成员领域专精度得分 (DES)】计算任务 ---");
        long desStartTime = System.currentTimeMillis();
        updateAllMemberRankings(allContentDTOsWithUpdatedCIS);
        timingStats.put("3. DES计算", System.currentTimeMillis() - desStartTime);

        log.info("--- 评级定时计算任务执行完毕。---");

        // 4. 成就检测
        try {
            log.info("--- 3. 开始执行【成就检测】计算任务 ---");
            long achievementStartTime = System.currentTimeMillis();
            achievementDetectionService.detectAndPersistAchievements();
            timingStats.put("4. 成就检测", System.currentTimeMillis() - achievementStartTime);
        } catch (Exception ex) {
            log.error("成就检测执行失败: {}", ex.getMessage(), ex);
            timingStats.put("4. 成就检测", System.currentTimeMillis() - System.currentTimeMillis());
        }

        log.info("--- 成就检测任务执行完毕。---");
        
        // 计算总耗时
        long totalTime = System.currentTimeMillis() - totalStartTime;
        timingStats.put("总耗时", totalTime);
        
        // 打印性能统计报告
        printPerformanceReport(timingStats);
    }

    /**
     * 新增方法：从论坛快照中同步成员数据。
     * 使用 REQUIRES_NEW 确保该方法在独立事务中运行，并在完成后立即提交，
     * 即使外部调用方 (executeDailyRatingCalculation) 没有事务或仍在事务中。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void syncMemberDataFromSnapshot() {
        long methodStartTime = System.currentTimeMillis();
        try {
            // 获取成员快照数据
            long pullStartTime = System.currentTimeMillis();
            List<Map<String, Object>> memberSnapshotMaps = forumDataSimulation.getMemberSnapshot();
            long pullTime = System.currentTimeMillis() - pullStartTime;
            
            if (memberSnapshotMaps.isEmpty()) {
                log.warn("未拉取到任何成员快照数据，跳过成员同步。");
                return;
            }
            
            log.info("  拉取成员快照耗时: {} ms, 数量: {}", pullTime, memberSnapshotMaps.size());
            
            // 使用进度条
            ProgressBar memberProgressBar = new ProgressBar("成员数据同步", memberSnapshotMaps.size());
            
            int newMemberCount = 0;
            long dbCheckTime = 0;
            long dbSaveTime = 0;
            
            // 遍历所有成员快照，检查并添加新成员
            for (Map<String, Object> memberMap : memberSnapshotMaps) {
                Long memberId = safeToLong(memberMap, "member_id");
                
                // 跳过无效的成员ID
                if (memberId == null) {
                    memberProgressBar.step();
                    continue;
                }
                
                // 检查成员是否已存在
                long checkStart = System.currentTimeMillis();
                Optional<Member> existingMember = memberRepository.findById(memberId);
                dbCheckTime += (System.currentTimeMillis() - checkStart);
                
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
                        // 如果没有提供加入日期，使用虚拟时间（支持时间模拟）
                        newMember.setJoinDate(TimeSimulation.now());
                    }
                    
                    // 保存新成员
                    long saveStart = System.currentTimeMillis();
                    memberRepository.save(newMember);
                    dbSaveTime += (System.currentTimeMillis() - saveStart);
                    newMemberCount++;
                    log.debug("新增成员: ID={}, 名称={}", memberId, newMember.getName());
                }
                
                memberProgressBar.step(); // 每处理一个成员步进一次
            }
            
            memberProgressBar.complete(); // 完成进度条
            
            long totalTime = System.currentTimeMillis() - methodStartTime;
            log.info("成员数据同步完成，新增 {} 个成员。", newMemberCount);
            log.info("  - 数据库检查耗时: {} ms", dbCheckTime);
            log.info("  - 数据库保存耗时: {} ms", dbSaveTime);
            log.info("  - 成员同步总耗时: {} ms", totalTime);
        } catch (Exception e) {
            log.error("成员数据同步失败: {}", e.getMessage(), e);
        }
        
        log.info("--- 成员数据同步任务执行完毕。---");
    }

    /**
     * 职责：计算所有内容的 CIS，并持久化到 ContentSnapshotRepository。
     * 流程：
     * 1. 拉取新快照并处理（有则插入，无则跳过）
     * 2. 无论是否有新快照，都遍历并更新数据库中所有现有的 ContentSnapshot 条目
     *    （因为分数随时间变化，需要重新计算衰减因子）
     * 返回：所有现有内容的更新后CIS列表（用于后续DES计算）
     */
    @Transactional
    private List<ContentDataDTO> calculateAllContentCIS() {
        long methodStartTime = System.currentTimeMillis();
        
        long pullStartTime = System.currentTimeMillis();
        List<Map<String, Object>> snapshotMaps = forumDataSimulation.getContentSnapshot();
        long pullTime = System.currentTimeMillis() - pullStartTime;
        
        final long[] mappingTime = {0};
        final long[] calculationTime = {0};
        final int[] filteredCount = {0};
        final int[] processedCount = {0};
        
        List<ContentDataDTO> calculatedContentDTOs = new java.util.ArrayList<>();
        
        // 第一步：处理新拉取的快照（如果有）
        long dbInsertStart = System.currentTimeMillis();
        if (!snapshotMaps.isEmpty()) {
            log.info("  拉取内容快照耗时: {} ms, 数量: {}", pullTime, snapshotMaps.size());
            ProgressBar cisProgressBar = new ProgressBar("内容影响力分数计算（新快照）", snapshotMaps.size());
            
            List<ContentSnapshot> newEntitiesToSave = new java.util.ArrayList<>();
            
            calculatedContentDTOs = snapshotMaps.stream()
                .map(map -> {
                    long mapStart = System.nanoTime();
                    ContentDataDTO dto = this.mapToDTO(map);
                    mappingTime[0] += (System.nanoTime() - mapStart) / 1_000_000;
                    return dto;
                })
                .filter(dto -> {
                    boolean hasAreaId = dto.getAreaId() != null;
                    if (!hasAreaId) filteredCount[0]++;
                    return hasAreaId;
                })
                .peek(dto -> {
                    long calcStart = System.nanoTime();
                    BigDecimal cisScore = ratingAlgorithm.calculateCIS(dto);
                    if (cisScore == null || cisScore.compareTo(BigDecimal.ZERO) < 0) {
                        cisScore = BigDecimal.ZERO;
                    }
                    calculationTime[0] += (System.nanoTime() - calcStart) / 1_000_000;
                    dto.setCisScore(cisScore);
                    
                    ContentSnapshot entity = convertToContentSnapshot(dto);
                    newEntitiesToSave.add(entity);
                    
                    processedCount[0]++;
                    if (processedCount[0] % 1000 == 0) {
                        cisProgressBar.increment(1000);
                    }
                })
                .collect(Collectors.toList());
            
            int remaining = calculatedContentDTOs.size() % 1000;
            if (remaining > 0) {
                cisProgressBar.increment(remaining);
            }
            cisProgressBar.complete();
            
            // 过滤新快照：只保留 member_id 已存在于 Member 表的记录
            Set<Long> existingMemberIds = newEntitiesToSave.stream()
                .map(ContentSnapshot::getMemberId)
                .collect(Collectors.toSet());
            Set<Long> validMemberIds = memberRepository.findAllById(new java.util.ArrayList<>(existingMemberIds)).stream()
                .map(Member::getMemberId)
                .collect(Collectors.toSet());
            
            List<ContentSnapshot> validNewEntities = newEntitiesToSave.stream()
                .filter(entity -> validMemberIds.contains(entity.getMemberId()))
                .collect(Collectors.toList());
            
            int skippedCount = newEntitiesToSave.size() - validNewEntities.size();
            if (skippedCount > 0) {
                log.warn("过滤掉 {} 条新内容（member_id 不存在于 Member 表）", skippedCount);
            }
            
            // 插入新快照：删除旧的相同 content_id，然后插入新的
            if (!validNewEntities.isEmpty()) {
                Set<Long> contentIdsToInsert = validNewEntities.stream()
                    .map(ContentSnapshot::getContentId)
                    .collect(Collectors.toSet());
                
                String deleteSQL = "DELETE FROM ContentSnapshot WHERE content_id IN (" +
                    String.join(",", contentIdsToInsert.stream().map(String::valueOf).toArray(String[]::new)) +
                    ")";
                int deletedCount = jdbcTemplate.update(deleteSQL);
                if (deletedCount > 0) {
                    log.info("删除 {} 条重复的 ContentSnapshot 记录", deletedCount);
                }
                
                // 批量插入新快照
                String insertSQL = """
                    INSERT INTO ContentSnapshot (
                        content_id, member_id, area_id, publish_time, post_length_level,
                        read_count_snapshot, like_count_snapshot, comment_count_snapshot,
                        share_count_snapshot, collect_count_snapshot, hate_count_snapshot,
                        cis_score
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                jdbcTemplate.batchUpdate(insertSQL, validNewEntities, validNewEntities.size(),
                    (ps, entity) -> {
                        ps.setLong(1, entity.getContentId());
                        ps.setLong(2, entity.getMemberId());
                        ps.setInt(3, entity.getAreaId());
                        ps.setTimestamp(4, java.sql.Timestamp.valueOf(entity.getPublishTime()));
                        ps.setInt(5, entity.getPostLengthLevel());
                        ps.setInt(6, entity.getReadCountSnapshot());
                        ps.setInt(7, entity.getLikeCountSnapshot());
                        ps.setInt(8, entity.getCommentCountSnapshot());
                        ps.setInt(9, entity.getShareCountSnapshot());
                        ps.setInt(10, entity.getCollectCountSnapshot());
                        ps.setInt(11, entity.getHateCountSnapshot());
                        ps.setBigDecimal(12, entity.getCisScore());
                    });
                log.info("批量插入 {} 条新的 ContentSnapshot 记录", validNewEntities.size());
            }
        }
        long dbInsertTime = System.currentTimeMillis() - dbInsertStart;
        
        // 第二步：遍历并更新数据库中所有现有的 ContentSnapshot 条目
        // （无论是否拉取到新快照，都要重新计算所有现有内容的分数）
        log.info("开始遍历并更新数据库中所有现有的 ContentSnapshot 条目...");
        
        String selectSQL = "SELECT content_id, member_id, area_id, publish_time, post_length_level, " +
            "read_count_snapshot, like_count_snapshot, comment_count_snapshot, share_count_snapshot, " +
            "collect_count_snapshot, hate_count_snapshot FROM ContentSnapshot";
        List<Map<String, Object>> allExistingSnapshots = jdbcTemplate.queryForList(selectSQL);
        log.info("查询到 {} 条现有 ContentSnapshot 记录", allExistingSnapshots.size());
        
        // 用于返回所有现有内容的更新后CIS列表
        List<ContentDataDTO> allExistingContentDTOsWithCIS = new java.util.ArrayList<>();
        
        if (allExistingSnapshots.isEmpty()) {
            log.info("数据库中没有现有的 ContentSnapshot 记录，跳过全量更新。");
        } else {
            ProgressBar updateProgressBar = new ProgressBar("内容影响力分数更新（所有现有）", allExistingSnapshots.size());
            
            List<ContentSnapshot> entitiesToUpdate = new java.util.ArrayList<>();
            final int[] updateFilteredCount = {0};
            final int[] updateProcessedCount = {0};
            
            // 收集所有更新后的DTO，用于后续DES计算
            allExistingContentDTOsWithCIS = allExistingSnapshots.stream()
                .map(map -> {
                    long mapStart = System.nanoTime();
                    ContentDataDTO dto = this.mapToDTOWithAreaId(map);  // 直接使用area_id
                    mappingTime[0] += (System.nanoTime() - mapStart) / 1_000_000;
                    return dto;
                })
                .filter(dto -> {
                    boolean hasAreaId = dto.getAreaId() != null;
                    if (!hasAreaId) updateFilteredCount[0]++;
                    return hasAreaId;
                })
                .map(dto -> {
                    long calcStart = System.nanoTime();
                    BigDecimal cisScore = ratingAlgorithm.calculateCIS(dto);
                    if (cisScore == null || cisScore.compareTo(BigDecimal.ZERO) < 0) {
                        cisScore = BigDecimal.ZERO;
                    }
                    calculationTime[0] += (System.nanoTime() - calcStart) / 1_000_000;
                    dto.setCisScore(cisScore);
                    
                    // 同时准备用于更新的Entity
                    ContentSnapshot entity = convertToContentSnapshot(dto);
                    entitiesToUpdate.add(entity);
                    
                    updateProcessedCount[0]++;
                    if (updateProcessedCount[0] % 1000 == 0) {
                        updateProgressBar.increment(1000);
                    }
                    
                    return dto;  // 返回更新后的DTO
                })
                .collect(Collectors.toList());
            
            log.info("第二阶段处理完成，收集到 {} 条DTO（过滤: {}），准备更新 {} 条Entity", 
                allExistingContentDTOsWithCIS.size(), updateFilteredCount[0], entitiesToUpdate.size());
            
            int updateRemaining = allExistingContentDTOsWithCIS.size() % 1000;
            if (updateRemaining > 0) {
                updateProgressBar.increment(updateRemaining);
            }
            updateProgressBar.complete();
            
            // 批量更新所有现有记录的 CIS 分数
            if (!entitiesToUpdate.isEmpty()) {
                String updateSQL = "UPDATE ContentSnapshot SET cis_score = ? WHERE content_id = ?";
                jdbcTemplate.batchUpdate(updateSQL, entitiesToUpdate, entitiesToUpdate.size(),
                    (ps, entity) -> {
                        ps.setBigDecimal(1, entity.getCisScore());
                        ps.setLong(2, entity.getContentId());
                    });
                log.info("批量更新 {} 条现有 ContentSnapshot 记录的 CIS 分数", entitiesToUpdate.size());
            }
        }
        
        long totalTime = System.currentTimeMillis() - methodStartTime;
        log.info("CIS计算完成，新快照有效内容: {}, 过滤: {}", calculatedContentDTOs.size(), filteredCount[0]);
        log.info("  - 数据映射耗时: {} ms", mappingTime[0]);
        log.info("  - CIS计算耗时: {} ms", calculationTime[0]);
        log.info("  - 数据库操作耗时: {} ms（插入: {} ms）", dbInsertTime + (totalTime - methodStartTime - dbInsertTime), dbInsertTime);
        log.info("  - CIS总耗时: {} ms", totalTime);
        log.info("  - 返回给DES计算的内容数: {}", allExistingContentDTOsWithCIS.size());
        
        // 返回所有现有内容的更新后CIS列表（用于DES计算）
        return allExistingContentDTOsWithCIS;
    }

    @Transactional
    private void updateAllMemberRankings(List<ContentDataDTO> contentDTOsWithCIS) {
        long methodStartTime = System.currentTimeMillis();
        
        if (contentDTOsWithCIS.isEmpty()) {
            log.warn("没有 CIS 数据，跳过 DES 计算。");
            return;
        }
        
        // 1. 按 (memberId, areaId) 对内容进行分组 (使用 Integer areaId)
        long groupStartTime = System.currentTimeMillis();
        Map<Long, Map<Integer, List<ContentDataDTO>>> memberContentGroup = contentDTOsWithCIS.stream()
            .collect(Collectors.groupingBy(
                ContentDataDTO::getMemberId,
                Collectors.groupingBy(ContentDataDTO::getAreaId)
            ));

        // 计算总处理组数用于进度条
        int totalGroups = memberContentGroup.values().stream()
            .mapToInt(Map::size)
            .sum();
        long groupTime = System.currentTimeMillis() - groupStartTime;
        
        log.info("  分组聚合耗时: {} ms, 成员-领域组合数: {}", groupTime, totalGroups);

        // 使用进度条
        ProgressBar desProgressBar = new ProgressBar("成员领域专精度得分计算", totalGroups);

        // 用于累积各操作的耗时
        final long[] desCalculationTime = {0};
        List<MemberRating> ratingsToInsert = new java.util.ArrayList<>();
        int insertCount = 0;

        // 2. 遍历每个成员和其在不同领域的内容，计算 DES
        for (Map.Entry<Long, Map<Integer, List<ContentDataDTO>>> memberEntry : memberContentGroup.entrySet()) {
            Long memberId = memberEntry.getKey();
            for (Map.Entry<Integer, List<ContentDataDTO>> areaEntry : memberEntry.getValue().entrySet()) {
                Integer areaId = areaEntry.getKey();
                List<ContentDataDTO> contents = areaEntry.getValue();
                
                long calcStart = System.nanoTime();
                BigDecimal desScore = ratingAlgorithm.calculateDES(contents);
                String ratingLevel = ratingAlgorithm.determineRatingLevel(desScore);
                desCalculationTime[0] += (System.nanoTime() - calcStart) / 1_000_000;
                
                // 创建新记录（保留历史记录，总是插入新行）
                MemberRating entity = new MemberRating();
                entity.setMemberId(memberId);
                entity.setAreaId(areaId);
                entity.setDesScore(desScore);
                entity.setRatingLevel(ratingLevel);
                entity.setUpdateDate(TimeSimulation.now().toLocalDate());
                ratingsToInsert.add(entity);
                insertCount++;
                
                desProgressBar.step();
            }
        }

        desProgressBar.complete();
        
        // 3. 批量插入新记录（使用 JdbcTemplate）
        long insertStartTime = System.currentTimeMillis();
        if (!ratingsToInsert.isEmpty()) {
            String sql = "INSERT INTO memberrating (member_id, area_id, des_score, rating_level, update_date) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.batchUpdate(sql, ratingsToInsert, ratingsToInsert.size(), 
                (ps, rating) -> {
                    ps.setLong(1, rating.getMemberId());
                    ps.setInt(2, rating.getAreaId());
                    ps.setBigDecimal(3, rating.getDesScore());
                    ps.setString(4, rating.getRatingLevel());
                    ps.setObject(5, rating.getUpdateDate());
                });
        }
        long insertTime = System.currentTimeMillis() - insertStartTime;
        
        long totalTime = System.currentTimeMillis() - methodStartTime;
        log.info("成员领域评分计算完成，新增: {} 条历史记录", insertCount);
        log.info("  - DES计算耗时: {} ms", desCalculationTime[0]);
        log.info("  - 批量插入耗时: {} ms", insertTime);
        log.info("  - DES总耗时: {} ms", totalTime);
    }

    /**
     * 打印性能统计报告
     */
    private void printPerformanceReport(Map<String, Long> timingStats) {
        log.info("========================================");
        log.info("       定时任务性能统计报告");
        log.info("========================================");
        
        // 按顺序打印各阶段耗时
        String[] orderedKeys = {
            "0. 缓存初始化",
            "1. 成员数据同步",
            "2. CIS计算",
            "3. DES计算",
            "4. 成就检测"
        };
        
        long totalMs = timingStats.getOrDefault("总耗时", 0L);
        
        for (String key : orderedKeys) {
            Long timeMs = timingStats.get(key);
            if (timeMs != null) {
                double percentage = totalMs > 0 ? (timeMs * 100.0 / totalMs) : 0;
                log.info("  {} : {} ms ({} 秒) - {}%", 
                    key, 
                    timeMs, 
                    String.format("%.2f", timeMs / 1000.0),
                    String.format("%.2f", percentage));
            }
        }
        
        log.info("----------------------------------------");
        log.info("  总耗时: {} ms ({} 秒 / {} 分钟)", 
            totalMs,
            String.format("%.2f", totalMs / 1000.0),
            String.format("%.2f", totalMs / 60000.0));
        log.info("========================================");
    }

    /**
     * 持久化单个 DTO 的 CIS 结果到数据库。
     */
    /**
     * 将 ContentDataDTO 转换为 ContentSnapshot 实体（不保存）
     */
    private ContentSnapshot convertToContentSnapshot(ContentDataDTO dto) {
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
        
        return entity;
    }


    /**
     * 职责：计算特定成员在特定领域 K 的 DES 分数和评级，并持久化。
     */
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
            
            // 映射其余字段 - publish_time 可能是 String 或 java.sql.Timestamp
            Object publishTimeObj = map.get("publish_time");
            if (publishTimeObj != null) {
                if (publishTimeObj instanceof String) {
                    dto.setPublishTime(LocalDateTime.parse((String) publishTimeObj));
                } else if (publishTimeObj instanceof java.sql.Timestamp) {
                    dto.setPublishTime(((java.sql.Timestamp) publishTimeObj).toLocalDateTime());
                }
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
    
    /**
     * 辅助方法：将 Map 结构转换为 DTO 对象，直接使用数据库中的 area_id（用于第二阶段）
     */
    private ContentDataDTO mapToDTOWithAreaId(Map<String, Object> map) {
        ContentDataDTO dto = new ContentDataDTO();
        try {
            dto.setContentId(safeToLong(map, "content_id"));
            dto.setMemberId(safeToLong(map, "member_id"));
            
            // 直接使用数据库中的 area_id，无需通过 knowledge_tag 转换
            Object areaIdObj = map.get("area_id");
            if (areaIdObj instanceof Number) {
                dto.setAreaId(((Number) areaIdObj).intValue());
            }
            
            // 映射其余字段 - publish_time 可能是 String 或 java.sql.Timestamp
            Object publishTimeObj = map.get("publish_time");
            if (publishTimeObj != null) {
                if (publishTimeObj instanceof String) {
                    dto.setPublishTime(LocalDateTime.parse((String) publishTimeObj));
                } else if (publishTimeObj instanceof java.sql.Timestamp) {
                    dto.setPublishTime(((java.sql.Timestamp) publishTimeObj).toLocalDateTime());
                }
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