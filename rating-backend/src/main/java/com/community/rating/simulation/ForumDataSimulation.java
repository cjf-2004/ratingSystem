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
    
    // 开关：是否使用静态测试数据（便于调试），默认 ture
    private volatile boolean useStaticTestData = true;


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
        // if (System.currentTimeMillis() % 50000 < 5000) {
        //     generateRandomMember();
        // }
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
          log.info("【评级系统 PULL】拉取内容快照 (全量)。 useStaticTestData={}", useStaticTestData);
          if (useStaticTestData) return getStaticContentSnapshot();
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
        log.info("【评级系统 PULL】拉取成员快照 (全量)。 useStaticTestData={}", useStaticTestData);
        if (useStaticTestData) return getStaticMemberSnapshot();
        return memberDB.values().stream()
               .map(m -> Map.<String, Object>of(
                       "member_id", m.id,
                       "name", m.name,
                       "join_date", m.joinDate.toString()
               ))
               .collect(Collectors.toList());
    }

    // Public setter to toggle static test data at runtime (e.g., from a test or a controller)
    public void setUseStaticTestData(boolean useStaticTestData) {
        this.useStaticTestData = useStaticTestData;
    }

    // ----------------- Static test data helpers -----------------
    public List<Map<String, Object>> getStaticMemberSnapshot() {
        List<Map<String, Object>> members = new ArrayList<>();
        java.util.HashMap<String, Object> m1 = new java.util.HashMap<>();
        m1.put("member_id", 101L); m1.put("name", "Alice_Frontend"); m1.put("join_date", "2024-11-16T09:00:00");
        members.add(m1);

        java.util.HashMap<String, Object> m2 = new java.util.HashMap<>();
        m2.put("member_id", 102L); m2.put("name", "Bob_Backend"); m2.put("join_date", "2023-11-16T09:00:00");
        members.add(m2);

        java.util.HashMap<String, Object> m3 = new java.util.HashMap<>();
        m3.put("member_id", 103L); m3.put("name", "Charlie_FS"); m3.put("join_date", "2025-01-01T09:00:00");
        members.add(m3);

        java.util.HashMap<String, Object> m4 = new java.util.HashMap<>();
        m4.put("member_id", 104L); m4.put("name", "Dana_Dev"); m4.put("join_date", "2022-06-01T09:00:00");
        members.add(m4);

        java.util.HashMap<String, Object> m5 = new java.util.HashMap<>();
        m5.put("member_id", 105L); m5.put("name", "Eve_Data"); m5.put("join_date", "2021-03-10T09:00:00");
        members.add(m5);

        return members;
    }

    public List<Map<String, Object>> getStaticContentSnapshot() {
        List<Map<String, Object>> list = new ArrayList<>();

        // Alice 连续 7 天的帖子（301..307）
        java.util.HashMap<String, Object> c301 = new java.util.HashMap<>();
        c301.put("content_id", 301L); c301.put("member_id", 101L); c301.put("publish_time", "2025-11-01T10:00:00");
        c301.put("knowledge_tag", "前端开发"); c301.put("post_length_level", 1); c301.put("read_count_snapshot", 5);
        c301.put("like_count_snapshot", 2); c301.put("comment_count_snapshot", 0); c301.put("share_count_snapshot", 0);
        c301.put("collect_count_snapshot", 0); c301.put("hate_count_snapshot", 0);
        list.add(c301);

        java.util.HashMap<String, Object> c302 = new java.util.HashMap<>();
        c302.put("content_id", 302L); c302.put("member_id", 101L); c302.put("publish_time", "2025-11-02T11:00:00");
        c302.put("knowledge_tag", "前端开发"); c302.put("post_length_level", 1); c302.put("read_count_snapshot", 6);
        c302.put("like_count_snapshot", 1); c302.put("comment_count_snapshot", 0); c302.put("share_count_snapshot", 0);
        c302.put("collect_count_snapshot", 0); c302.put("hate_count_snapshot", 0);
        list.add(c302);

        java.util.HashMap<String, Object> c303 = new java.util.HashMap<>();
        c303.put("content_id", 303L); c303.put("member_id", 101L); c303.put("publish_time", "2025-11-03T12:00:00");
        c303.put("knowledge_tag", "前端开发"); c303.put("post_length_level", 2); c303.put("read_count_snapshot", 8);
        c303.put("like_count_snapshot", 3); c303.put("comment_count_snapshot", 1); c303.put("share_count_snapshot", 0);
        c303.put("collect_count_snapshot", 0); c303.put("hate_count_snapshot", 0);
        list.add(c303);

        java.util.HashMap<String, Object> c304 = new java.util.HashMap<>();
        c304.put("content_id", 304L); c304.put("member_id", 101L); c304.put("publish_time", "2025-11-04T09:30:00");
        c304.put("knowledge_tag", "前端开发"); c304.put("post_length_level", 2); c304.put("read_count_snapshot", 12);
        c304.put("like_count_snapshot", 4); c304.put("comment_count_snapshot", 0); c304.put("share_count_snapshot", 1);
        c304.put("collect_count_snapshot", 0); c304.put("hate_count_snapshot", 0);
        list.add(c304);

        java.util.HashMap<String, Object> c305 = new java.util.HashMap<>();
        c305.put("content_id", 305L); c305.put("member_id", 101L); c305.put("publish_time", "2025-11-05T14:00:00");
        c305.put("knowledge_tag", "前端开发"); c305.put("post_length_level", 1); c305.put("read_count_snapshot", 20);
        c305.put("like_count_snapshot", 5); c305.put("comment_count_snapshot", 0); c305.put("share_count_snapshot", 0);
        c305.put("collect_count_snapshot", 0); c305.put("hate_count_snapshot", 0);
        list.add(c305);

        java.util.HashMap<String, Object> c306 = new java.util.HashMap<>();
        c306.put("content_id", 306L); c306.put("member_id", 101L); c306.put("publish_time", "2025-11-06T08:00:00");
        c306.put("knowledge_tag", "前端开发"); c306.put("post_length_level", 1); c306.put("read_count_snapshot", 9);
        c306.put("like_count_snapshot", 2); c306.put("comment_count_snapshot", 0); c306.put("share_count_snapshot", 0);
        c306.put("collect_count_snapshot", 0); c306.put("hate_count_snapshot", 0);
        list.add(c306);

        java.util.HashMap<String, Object> c307 = new java.util.HashMap<>();
        c307.put("content_id", 307L); c307.put("member_id", 101L); c307.put("publish_time", "2025-11-07T20:00:00");
        c307.put("knowledge_tag", "前端开发"); c307.put("post_length_level", 1); c307.put("read_count_snapshot", 11);
        c307.put("like_count_snapshot", 6); c307.put("comment_count_snapshot", 2); c307.put("share_count_snapshot", 0);
        c307.put("collect_count_snapshot", 0); c307.put("hate_count_snapshot", 0);
        list.add(c307);

        // Charlie 单条热门 (>=100 like, >=50 comments)
        java.util.HashMap<String, Object> c310 = new java.util.HashMap<>();
        c310.put("content_id", 310L); c310.put("member_id", 103L); c310.put("publish_time", "2025-10-15T10:00:00");
        c310.put("knowledge_tag", "后端开发"); c310.put("post_length_level", 2); c310.put("read_count_snapshot", 200);
        c310.put("like_count_snapshot", 120); c310.put("comment_count_snapshot", 60); c310.put("share_count_snapshot", 5);
        c310.put("collect_count_snapshot", 2); c310.put("hate_count_snapshot", 0);
        list.add(c310);

        // Dana 超级热门 (>=1000 like)
        java.util.HashMap<String, Object> c320 = new java.util.HashMap<>();
        c320.put("content_id", 320L); c320.put("member_id", 104L); c320.put("publish_time", "2025-09-01T09:00:00");
        c320.put("knowledge_tag", "云计算与运维"); c320.put("post_length_level", 3); c320.put("read_count_snapshot", 5000);
        c320.put("like_count_snapshot", 1200); c320.put("comment_count_snapshot", 300); c320.put("share_count_snapshot", 200);
        c320.put("collect_count_snapshot", 50); c320.put("hate_count_snapshot", 1);
        list.add(c320);

        // Eve 多条累计点赞 >= 600
        java.util.HashMap<String, Object> c330 = new java.util.HashMap<>();
        c330.put("content_id", 330L); c330.put("member_id", 105L); c330.put("publish_time", "2025-07-01T12:00:00");
        c330.put("knowledge_tag", "数据科学"); c330.put("post_length_level", 2); c330.put("read_count_snapshot", 100);
        c330.put("like_count_snapshot", 250); c330.put("comment_count_snapshot", 10); c330.put("share_count_snapshot", 5);
        c330.put("collect_count_snapshot", 0); c330.put("hate_count_snapshot", 0);
        list.add(c330);

        java.util.HashMap<String, Object> c331 = new java.util.HashMap<>();
        c331.put("content_id", 331L); c331.put("member_id", 105L); c331.put("publish_time", "2025-08-01T12:00:00");
        c331.put("knowledge_tag", "数据科学"); c331.put("post_length_level", 1); c331.put("read_count_snapshot", 50);
        c331.put("like_count_snapshot", 200); c331.put("comment_count_snapshot", 5); c331.put("share_count_snapshot", 2);
        c331.put("collect_count_snapshot", 0); c331.put("hate_count_snapshot", 0);
        list.add(c331);

        java.util.HashMap<String, Object> c332 = new java.util.HashMap<>();
        c332.put("content_id", 332L); c332.put("member_id", 105L); c332.put("publish_time", "2025-09-01T12:00:00");
        c332.put("knowledge_tag", "数据科学"); c332.put("post_length_level", 1); c332.put("read_count_snapshot", 30);
        c332.put("like_count_snapshot", 150); c332.put("comment_count_snapshot", 3); c332.put("share_count_snapshot", 1);
        c332.put("collect_count_snapshot", 0); c332.put("hate_count_snapshot", 0);
        list.add(c332);

        return list;
    }
}