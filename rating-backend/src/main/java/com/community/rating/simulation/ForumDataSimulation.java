package com.community.rating.simulation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.Random;

/**
 * 【重构组件】论坛系统模拟器：完全模拟外部论坛数据库和数据接口。
 * 职责：
 * 1. 持续生成并维护 Member, Content, InteractionEvent 三类实体数据。
 * 2. 对外暴露：评级系统定时拉取数据的接口 (PULL)。
 * 3. 模拟：外部系统偶尔向评级系统推送通知 (PUSH)。
 * 数据结构已严格对齐评级系统的 ContentSnapshot 和 Member 表要求。
 */
@Service
public class ForumDataSimulation {

    private static final Logger log = LoggerFactory.getLogger(ForumDataSimulation.class);
    private final Random random = new Random();

    // --- 模拟数据存储 (模拟论坛实时数据库) ---
    private final Map<Long, MemberRecord> memberDB = new ConcurrentHashMap<>();
    private final Map<Long, ContentRecord> contentDB = new ConcurrentHashMap<>();
    private final List<InteractionEventRecord> eventLog = Collections.synchronizedList(new ArrayList<>());

    // 唯一 ID 生成器
    private final AtomicLong memberIdCounter = new AtomicLong(103);
    private final AtomicLong contentIdCounter = new AtomicLong(200);
    private final AtomicLong eventIdCounter = new AtomicLong(1);

    // 知识领域映射 (使用 String 作为知识领域标签)
    private static final List<String> KNOWLEDGE_AREA_TAGS = List.of(
            "前端开发", "后端开发", "数据科学", "视觉设计", "产品管理", "云计算与运维");

    // 内部数据实体定义 (使用 Record 模拟)
    // MemberRecord 匹配 Member 表
    private record MemberRecord(Long id, String name, LocalDateTime joinDate) {} 

    // ContentRecord 匹配 ContentSnapshot 表（不含 cis_score）
    private record ContentRecord(
        Long id, 
        Long authorId, 
        String title, 
        LocalDateTime publishTime, 
        String knowledgeTag,         // knowledge_tag
        int postLengthLevel,         // post_length_level (1, 2, 3)
        // 快照计数器，用于实时累积
        AtomicLong readCount,        // read_count_snapshot
        AtomicLong likeCount,        // like_count_snapshot
        AtomicLong commentCount,     // comment_count_snapshot
        AtomicLong shareCount,       // share_count_snapshot
        AtomicLong collectCount,     // collect_count_snapshot
        AtomicLong hateCount         // hate_count_snapshot
    ) {}

    // InteractionType 扩展为 6 种类型
    private record InteractionEventRecord(Long eventId, Long contentId, Long memberId, InteractionType type, LocalDateTime timestamp) {}
    private enum InteractionType { LIKE, COMMENT, SHARE, READ, COLLECT, HATE }
    
    // --- 初始化和持续活动模拟 ---

    public ForumDataSimulation() {
        // 初始化一些基础成员数据 (匹配 Member 表要求)
        memberDB.put(101L, new MemberRecord(101L, "Alice_Frontend", LocalDateTime.now().minusYears(1)));
        memberDB.put(102L, new MemberRecord(102L, "Bob_Backend", LocalDateTime.now().minusYears(2)));
        memberDB.put(103L, new MemberRecord(103L, "Charlie_FullStack", LocalDateTime.now().minusMonths(3)));

        // 初始化一些内容 (匹配 ContentSnapshot 表要求)
        initializeContent(201L, 101L, "React 状态管理最佳实践", "前端开发", 3, LocalDateTime.now().minusDays(1));
        initializeContent(202L, 102L, "Spring Boot 性能优化", "后端开发", 2, LocalDateTime.now().minusHours(10));
    }
    
    // 辅助初始化方法
    private void initializeContent(Long id, Long authorId, String title, String tag, int lengthLevel, LocalDateTime time) {
        ContentRecord content = new ContentRecord(
            id, authorId, title, time, tag, lengthLevel,
            new AtomicLong(10), // 初始阅读数
            new AtomicLong(0),
            new AtomicLong(0),
            new AtomicLong(0),
            new AtomicLong(0),
            new AtomicLong(0)
        );
        contentDB.put(id, content);
    }
    
    /**
     * 【持续活动模拟】定时任务：每 5 秒生成一次新的论坛活动。
     * 模拟持续数据写入。
     */
    @Scheduled(fixedRate = 5000)
    public void simulateContinuousActivity() {
        // 随机生成互动事件
        generateRandomInteraction();
        
        // 偶尔生成新成员 (每 10 次活动模拟生成 1 次新成员，即约 50 秒一次)
        if (System.currentTimeMillis() % 50000 < 5000) {
            generateRandomMember();
        }
        // 偶尔生成新内容 (每 5 次活动模拟生成 1 次新内容)
        if (System.currentTimeMillis() % 25000 < 5000) {
             generateRandomContent();
        }
    }
    /**
     * 新增方法：随机生成一个新的用户（MemberRecord）。
     */
    private void generateRandomMember() {
        Long newMemberId = memberIdCounter.incrementAndGet();
        
        // 随机选择一个知识领域标签作为后缀，让名字更有区分度
        String randomTag = KNOWLEDGE_AREA_TAGS.get(random.nextInt(KNOWLEDGE_AREA_TAGS.size()));
        String newName = "User_" + newMemberId + "_" + randomTag.replace("开发", "");
        
        MemberRecord newMember = new MemberRecord(
                newMemberId, 
                newName, 
                LocalDateTime.now()
        );
        memberDB.put(newMemberId, newMember);
        log.info("【新成员】注册成功: ID={}, 姓名={}, 加入时间={}", newMemberId, newName, newMember.joinDate().toLocalTime());
    }
    private void generateRandomInteraction() {
        if (contentDB.isEmpty() || memberDB.isEmpty()) return;

        // 随机选择一个已发布内容
        Long[] contentIds = contentDB.keySet().toArray(new Long[0]);
        Long contentId = contentIds[random.nextInt(contentIds.length)];

        // 随机选择一个成员
        Long[] memberIds = memberDB.keySet().toArray(new Long[0]); 
        Long memberId = memberIds[random.nextInt(memberIds.length)];

        // 随机选择一个互动类型 (6种类型)
        InteractionType[] types = InteractionType.values();
        InteractionType type = types[random.nextInt(types.length)];

        // 1. 创建新事件 (用于评级系统批量拉取)
        InteractionEventRecord newEvent = new InteractionEventRecord(
                eventIdCounter.incrementAndGet(),
                contentId,
                memberId,
                type,
                LocalDateTime.now()
        );
        
        eventLog.add(newEvent);
        log.debug("生成互动事件: 内容ID={}, 成员ID={}, 类型={}", contentId, memberId, type);

        // 2. 【实时更新快照】更新 ContentRecord 中的累积计数
        ContentRecord content = contentDB.get(contentId);
        if (content != null) {
            switch (type) {
                case READ -> content.readCount().incrementAndGet();
                case LIKE -> content.likeCount().incrementAndGet(); // 移除 checkAchievement() 调用
                case COMMENT -> content.commentCount().incrementAndGet();
                case SHARE -> content.shareCount().incrementAndGet();
                case COLLECT -> content.collectCount().incrementAndGet();
                case HATE -> content.hateCount().incrementAndGet();
            }
        }
    }
    
    private void generateRandomContent() {
        Long newMemberId = memberDB.keySet().stream().skip(random.nextInt(memberDB.size())).findFirst().orElse(101L);
        Long newContentId = contentIdCounter.incrementAndGet();

        // 随机分配知识领域标签
        String randomTag = KNOWLEDGE_AREA_TAGS.get(random.nextInt(KNOWLEDGE_AREA_TAGS.size()));
        
        // 随机分配帖子长度等级 (1, 2, or 3)
        int lengthLevel = random.nextInt(3) + 1; 

        ContentRecord newContent = new ContentRecord(
                newContentId,
                newMemberId,
                "新内容标题-" + newContentId,
                LocalDateTime.now(),
                randomTag,
                lengthLevel,
                new AtomicLong(random.nextInt(10) + 1), // 初始阅读数
                new AtomicLong(0),
                new AtomicLong(0),
                new AtomicLong(0),
                new AtomicLong(0),
                new AtomicLong(0)
        );
        contentDB.put(newContentId, newContent);
        log.info("生成新内容: ID={}, 作者={}, 领域={}, 长度={}", newContentId, newMemberId, randomTag, lengthLevel);
    }
    
    
    // --- 数据源接口与交互规范：批量评级数据拉取规范 ---
    
    /**
     * 接口：拉取在特定时间窗口内产生的所有互动事件记录。
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 互动事件列表（DTOs）
     */
    public List<Map<String, Object>> getAllInteractionEventsInWindow(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("【评级系统 PULL】拉取互动事件：从 {} 到 {}", startTime, endTime);

        // 过滤事件日志
        return eventLog.stream()
                .filter(e -> e.timestamp.isAfter(startTime) && e.timestamp.isBefore(endTime))
                .map(e -> Map.<String, Object>of(
                        "eventId", e.eventId,
                        "contentId", e.contentId,
                        "memberId", e.memberId,
                        "type", e.type.name(),
                        "timestamp", e.timestamp.toString()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 辅助方法：将 ContentRecord 映射为 ContentSnapshot 结构。
     */
    private Map<String, Object> mapContentRecordToSnapshot(ContentRecord c) {
        // 使用 HashMap 替代 Map.of() 来支持超过 10 个键值对。
        Map<String, Object> snapshot = new java.util.HashMap<>();
        snapshot.put("content_id", c.id);
        snapshot.put("member_id", c.authorId);
        snapshot.put("publish_time", c.publishTime.toString());
        snapshot.put("knowledge_tag", c.knowledgeTag);
        snapshot.put("post_length_level", c.postLengthLevel);
        // 快照计数
        snapshot.put("read_count_snapshot", c.readCount.get());
        snapshot.put("like_count_snapshot", c.likeCount.get());
        snapshot.put("comment_count_snapshot", c.commentCount.get());
        snapshot.put("share_count_snapshot", c.shareCount.get());
        snapshot.put("collect_count_snapshot", c.collectCount.get());
        snapshot.put("hate_count_snapshot", c.hateCount.get());
        return snapshot;
    }

    /**
     * 接口：提供最新的全量内容数据快照。
     * 返回结构已对齐 ContentSnapshot 表结构（不含 cis_score）。
     * @return 内容快照列表（DTOs）
     */
    public List<Map<String, Object>> getContentSnapshot() {
         log.info("【评级系统 PULL】拉取内容快照 (全量)。");
         return contentDB.values().stream()
                .map(this::mapContentRecordToSnapshot) // 使用方法引用，明确类型
                .collect(Collectors.toList());
    }

    /**
     * 接口：提供最新的全量成员数据快照。
     * 返回结构已对齐 Member 表结构。
     * @return 成员快照列表（DTOs）
     */
    public List<Map<String, Object>> getMemberSnapshot() {
        log.info("【评级系统 PULL】拉取成员快照 (全量)。");
        return memberDB.values().stream()
               .map(m -> Map.<String, Object>of(
                       "member_id", m.id,
                       "name", m.name,
                       "join_date", m.joinDate.toString()
               ))
               .collect(Collectors.toList());
    }
}