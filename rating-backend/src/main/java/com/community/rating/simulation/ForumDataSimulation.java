package com.community.rating.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Getter;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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


    // 知识领域映射 (使用 String 作为知识领域标签)
    private static final List<String> KNOWLEDGE_AREA_TAGS = List.of(
            "前端开发", "后端开发", "数据科学", "视觉设计", "产品管理", "云计算与运维");

    // 模拟用户行为特征定义
    private static final int GENERATE_DAYS_RANGE = 300; // 生成数据的时间范围（天）, 需大于180
    // 起始和结束时间
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDateTime START_TIME = NOW.minusDays(GENERATE_DAYS_RANGE);
    // 生成用户量
    private static final int SIMULATE_USER_COUNT = 2000;
    // 是否为保存模式
    private static final boolean IS_SAVE_MODE = true;
    // 是否为读取模式
    private static final boolean IS_LOAD_MODE = false;
    // 模拟行为文件夹
    private static final String SIMULATION_DATA_FOLDER = "./simulation/";
    // 模拟行为文件
    private static final String SIMULATION_DATA_FILE = SIMULATION_DATA_FOLDER + "forum_simulation_data.json";
    // 用户行为枚举
    private static final WeightedList<UserBehavior> SIMULATE_USER_BEHAVIORS = new WeightedList<>(List.of(
            new Pair<>(new UserBehavior(
                    "量产低质型",
                    new WeightedList<>(List.of(60, 90, 150), List.of(50, 35, 15)),
                    new WeightedList<>(List.of(0), List.of(1)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(80, 18, 2)),
                    new InteractionMetrics(800, 250, 0.01, 0.002, 0.001, 0.003, 0.025),
                    Optional.empty()
            ), 6),
            new Pair<>(new UserBehavior(
                    "稳健创作型",
                    new WeightedList<>(List.of(24, 36, 60), List.of(60, 30, 10)),
                    new WeightedList<>(List.of(0, 1), List.of(70, 30)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(20, 60, 20)),
                    new InteractionMetrics(8000, 3000, 0.05, 0.015, 0.008, 0.012, 0.004),
                    Optional.of(new SpecialPostRules(0.01, 5.0, 1.6, 1.6, 1.6, 1.6, 1.6))
            ), 22),
            new Pair<>(new UserBehavior(
                    "爆款拉动型",
                    new WeightedList<>(List.of(24, 36, 72), List.of(50, 40, 10)),
                    new WeightedList<>(List.of(0, 1), List.of(80, 20)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(30, 50, 20)),
                    new InteractionMetrics(4000, 2500, 0.04, 0.01, 0.006, 0.01, 0.005),
                    Optional.of(new SpecialPostRules(0.08, 10.0, 1.8, 1.8, 1.8, 1.8, 1.8))
            ), 12),
            new Pair<>(new UserBehavior(
                    "长文深度收藏型",
                    new WeightedList<>(List.of(12, 18, 30), List.of(70, 25, 5)),
                    new WeightedList<>(List.of(0, 1), List.of(90, 10)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(5, 35, 60)),
                    new InteractionMetrics(5500, 2000, 0.05, 0.03, 0.012, 0.06, 0.003),
                    Optional.empty()
            ), 8),
            new Pair<>(new UserBehavior(
                    "传播导向型",
                    new WeightedList<>(List.of(36, 48, 80), List.of(50, 35, 15)),
                    new WeightedList<>(List.of(0, 1), List.of(60, 40)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(70, 25, 5)),
                    new InteractionMetrics(12000, 6000, 0.03, 0.006, 0.05, 0.006, 0.006),
                    Optional.empty()
            ), 8),
            new Pair<>(new UserBehavior(
                    "讨论驱动型",
                    new WeightedList<>(List.of(18, 24, 36), List.of(60, 30, 10)),
                    new WeightedList<>(List.of(0, 1, 2), List.of(70, 20, 10)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(10, 55, 35)),
                    new InteractionMetrics(4500, 2200, 0.035, 0.05, 0.008, 0.018, 0.007),
                    Optional.empty()
            ), 8),
            new Pair<>(new UserBehavior(
                    "争议两极型",
                    new WeightedList<>(List.of(12, 16, 30), List.of(60, 30, 10)),
                    new WeightedList<>(List.of(0, 1), List.of(70, 30)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(30, 50, 20)),
                    new InteractionMetrics(6000, 4000, 0.03, 0.05, 0.01, 0.01, 0.03),
                    Optional.of(new SpecialPostRules(0.2, 4.0, 2.0, 2.0, 2.0, 1.0, 2.0))
            ), 5),
            new Pair<>(new UserBehavior(
                    "标题党低转化型",
                    new WeightedList<>(List.of(20, 30, 50), List.of(50, 35, 15)),
                    new WeightedList<>(List.of(0, 1), List.of(60, 40)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(70, 25, 5)),
                    new InteractionMetrics(30000, 15000, 0.007, 0.002, 0.005, 0.001, 0.012),
                    Optional.empty()
            ), 12),
            new Pair<>(new UserBehavior(
                    "单领域精品型",
                    new WeightedList<>(List.of(10, 15, 24), List.of(70, 25, 5)),
                    new WeightedList<>(List.of(0), List.of(1)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(10, 40, 50)),
                    new InteractionMetrics(2500, 900, 0.04, 0.018, 0.012, 0.07, 0.002),
                    Optional.of(new SpecialPostRules(0.25, 3.0, 1.5, 1.5, 1.5, 2.0, 1.5))
            ), 7),
            new Pair<>(new UserBehavior(
                    "单领域勤奋型",
                    new WeightedList<>(List.of(30, 45, 70), List.of(40, 40, 20)),
                    new WeightedList<>(List.of(0, 1), List.of(95, 5)),
                    new WeightedList<>(List.of(1, 2, 3), List.of(30, 60, 10)),
                    new InteractionMetrics(2000, 700, 0.025, 0.006, 0.004, 0.013, 0.005),
                    Optional.empty()
            ), 12)
    ));

    private static final Logger log = LoggerFactory.getLogger(ForumDataSimulation.class);
    private final Random random = new Random();

    // --- 模拟数据存储 (模拟论坛实时数据库) ---
    private final Map<Long, MemberRecord> memberDB = new ConcurrentHashMap<>();
    private final Map<Long, ContentRecord> contentDB = new ConcurrentHashMap<>();
    private final List<InteractionEventRecord> eventLog = Collections.synchronizedList(new ArrayList<>());

    // 模拟保存数据结构
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 唯一 ID 生成器
    private final AtomicLong memberIdCounter = new AtomicLong(103);
    private final AtomicLong contentIdCounter = new AtomicLong(200);
    private final AtomicLong eventIdCounter = new AtomicLong(1);

    // 内部数据实体定义 (使用 Record 模拟)
    // MemberRecord 匹配 Member 表
    private record MemberRecord(long id, String name, LocalDateTime joinDate) {}

    // ContentRecord 匹配 ContentSnapshot 表（不含 cis_score）
    private record ContentRecord(
            long id,
            long authorId,
            String title,
            LocalDateTime publishTime,
            String knowledgeTag,         // knowledge_tag
            int postLengthLevel,         // post_length_level (1, 2, 3)
            // 快照计数器，用于实时累积
            long readCount,        // read_count_snapshot
            long likeCount,        // like_count_snapshot
            long commentCount,     // comment_count_snapshot
            long shareCount,       // share_count_snapshot
            long collectCount,     // collect_count_snapshot
            long hateCount         // hate_count_snapshot
    ) {}

    // InteractionType 扩展为 6 种类型
    private record InteractionEventRecord(Long eventId, Long contentId, Long memberId, InteractionType type, LocalDateTime timestamp) {}
    private enum InteractionType { LIKE, COMMENT, SHARE, READ, COLLECT, HATE }

    private record UserBehavior(
            String behaviorName, // 行为名称
            WeightedList<Integer> postCountBase, // 发帖数基准
            WeightedList<Integer> domains, // 领域分布
            WeightedList<Integer> lengthLevels, // 文章长度分布
            InteractionMetrics interaction, // 互动基准
            Optional<ExtraRules> extraRules // 额外规则
    ) {}

    private record InteractionMetrics(
            double readMean, double readStdDev, double likePercent, double commentPercent,
            double sharePercent, double collectPercent, double hatePercent
    ) {}

    private record InteractionCount(
            long readCount, long likeCount, long commentCount,
            long shareCount, long collectCount, long hateCount
    ) {}

    // 占位符，未来可扩展额外规则
    private interface ExtraRules {}

    // 特殊文章规则
    private record SpecialPostRules(
            double percent,
            double readMultiplier, double likeMultiplier,
            double commentMultiplier, double shareMultiplier,
            double collectMultiplier, double hateMultiplier
    ) implements ExtraRules {}


    private record SimulationSaveData(
            List<MemberRecord> members,
            List<ContentRecord> contents,
            LocalDateTime timestamp
    ) {}

    // --- 初始化 ---
    public ForumDataSimulation() {
//        // 初始化一些基础成员数据 (匹配 Member 表要求)
//        memberDB.put(101L, new MemberRecord(101L, "Alice_Frontend", LocalDateTime.now().minusYears(1)));
//        memberDB.put(102L, new MemberRecord(102L, "Bob_Backend", LocalDateTime.now().minusYears(2)));
//        memberDB.put(103L, new MemberRecord(103L, "Charlie_FullStack", LocalDateTime.now().minusMonths(3)));
//
//        // 初始化一些内容 (匹配 ContentSnapshot 表要求)
//        initializeContent(201L, 101L, "React 状态管理最佳实践", "前端开发", 3, LocalDateTime.now().minusDays(1));
//        initializeContent(202L, 102L, "Spring Boot 性能优化", "后端开发", 2, LocalDateTime.now().minusHours(10));
        configureObjectMapper();

        createFilesIfNotExist();
        if (IS_LOAD_MODE) {
            loadFromFile();
        } else {
            simulate();
            if (IS_SAVE_MODE) {
                // 保存数据到文件
                saveToFile();
            }
        }
    }

    // 主模拟方法
    private void simulate() {
        int[] distributes = SIMULATE_USER_BEHAVIORS.getDistributeList(SIMULATE_USER_COUNT);
        for (int i = 0; i < SIMULATE_USER_BEHAVIORS.size(); i++) {
            UserBehavior behavior = SIMULATE_USER_BEHAVIORS.getElements().get(i);
            int userCount = distributes[i];
            log.info("生成用户行为类型 '{}' 的用户，共计 {} 人。", behavior.behaviorName, userCount);
            for (int j = 0; j < userCount; j++) {
                generateUserOfBehavior(behavior);
            }
        }
    }

    // 根据用户特征生成用户
    private void generateUserOfBehavior(UserBehavior behavior) {
        // 计算需要抽取的领域数
        int domainCount = behavior.domains.size();
        // 抽取领域
        List<String> domains = sample(KNOWLEDGE_AREA_TAGS, domainCount);
        // 生成用户
        MemberRecord memberRecord = generateRandomMember(behavior.behaviorName, domains.get(0));

        // 计算需要生成的文章数
        int articlesToGenerate = randAround(sample(behavior.postCountBase), 10);
        // 生成文章发布时间
        List<LocalDateTime> publishDates = sampleDates(START_TIME, GENERATE_DAYS_RANGE, articlesToGenerate);

        for (LocalDateTime publishDate : publishDates) {
            // 随机选择领域和长度等级
            String domainTag = domains.get(sample(behavior.domains));
            int lengthLevel = sample(behavior.lengthLevels);

            // 生成阅读量, 正态分布
            long readCount = (long) Math.max(0, normalRandom(behavior.interaction.readMean, behavior.interaction.readStdDev));

            InteractionCount interactionCount = new InteractionCount(
                    readCount,
                    randAround((long) (readCount * behavior.interaction.likePercent), 10),
                    randAround((long) (readCount * behavior.interaction.commentPercent), 10),
                    randAround((long) (readCount * behavior.interaction.sharePercent), 10),
                    randAround((long) (readCount * behavior.interaction.collectPercent), 10),
                    randAround((long) (readCount * behavior.interaction.hatePercent), 10)
            );

            // 检查是否应用特殊文章规则
            if (behavior.extraRules.isPresent()) {
                ExtraRules extraRules = behavior.extraRules.get();
                if (extraRules instanceof SpecialPostRules specialRules) {
                    // 抽中
                    if (random.nextDouble() < specialRules.percent) {
                        interactionCount = new InteractionCount(
                                (long) (interactionCount.readCount * specialRules.readMultiplier),
                                (long) (interactionCount.likeCount * specialRules.likeMultiplier),
                                (long) (interactionCount.commentCount * specialRules.commentMultiplier),
                                (long) (interactionCount.shareCount * specialRules.shareMultiplier),
                                (long) (interactionCount.collectCount * specialRules.collectMultiplier),
                                (long) (interactionCount.hateCount * specialRules.hateMultiplier)
                        );
                    }
                } else {
                    log.warn("未知的额外规则类型: {}", extraRules.getClass().getName());
                    throw new IllegalArgumentException("未知的额外规则类型");
                }
            }

            // 生成内容记录
            ContentRecord content = generateContent(
                    memberRecord.id,
                    domainTag,
                    publishDate,
                    lengthLevel,
                    interactionCount
            );

            contentDB.put(content.id, content);
            logContent(content);
        }

    }
    
    // 辅助初始化方法
    private ContentRecord generateContent(
            long authorId,
            String knowledgeTag,
            LocalDateTime publishTime,
            int lengthLevel,
            InteractionCount interaction

    ) {
        long newContentId = contentIdCounter.incrementAndGet();

        return new ContentRecord(
                newContentId,
                authorId,
                "新内容标题-" + newContentId,
                publishTime,
                knowledgeTag,
                lengthLevel,
                interaction.readCount,
                interaction.likeCount,
                interaction.commentCount,
                interaction.shareCount,
                interaction.collectCount,
                interaction.hateCount
        );
    }

    private void createFilesIfNotExist() {
        try {
            File folder = new File(SIMULATION_DATA_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File file = new File(SIMULATION_DATA_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            log.error("创建文件时出错: {}", e.getMessage());
        }
    }

    private void configureObjectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule(); // 注册 Java 时间模块
        javaTimeModule.addSerializer(
                LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        javaTimeModule.addDeserializer(
                LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        objectMapper.registerModule(javaTimeModule);
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private void saveToFile() {
        SimulationSaveData saveData = new SimulationSaveData(
                new ArrayList<>(memberDB.values()),
                new ArrayList<>(contentDB.values()),
                NOW
        );
        try {
            objectMapper.writeValue(new FileOutputStream(SIMULATION_DATA_FILE), saveData);
            log.info("模拟数据已保存到 {} 文件。", SIMULATION_DATA_FILE);
        } catch (Exception e) {
            log.error("保存模拟数据时出错: {}", e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            SimulationSaveData saveData = objectMapper.readValue(new FileInputStream(SIMULATION_DATA_FILE), SimulationSaveData.class);
            for (MemberRecord member : saveData.members()) {
                memberDB.put(member.id, member);
            }
            // 计算时间差
            long days = ChronoUnit.DAYS.between(saveData.timestamp, NOW);
            for (ContentRecord content : saveData.contents()) {
                if (days > 0) {
                    contentDB.put(content.id, new ContentRecord(
                            content.id,
                            content.authorId,
                            content.title,
                            content.publishTime.plusDays(days),
                            content.knowledgeTag,
                            content.postLengthLevel,
                            content.readCount,
                            content.likeCount,
                            content.commentCount,
                            content.shareCount,
                            content.collectCount,
                            content.hateCount
                    ));
                } else {
                    contentDB.put(content.id, content);
                }
            }
            log.info("模拟数据已从 {} 文件加载。", SIMULATION_DATA_FILE);
        } catch (Exception e) {
            log.error("加载模拟数据时出错: {}", e.getMessage());
        }
    }
    
//    /**
//     * 【持续活动模拟】定时任务：每 5 秒生成一次新的论坛活动。
//     * 模拟持续数据写入。
//     */
//    @Scheduled(fixedRate = 5000)
//    public void simulateContinuousActivity() {
//        // 随机生成互动事件
//        generateRandomInteraction();
//
//        // 偶尔生成新成员 (每 10 次活动模拟生成 1 次新成员，即约 50 秒一次)
//        if (System.currentTimeMillis() % 50000 < 5000) {
//            generateRandomMember();
//        }
//        // 偶尔生成新内容 (每 5 次活动模拟生成 1 次新内容)
//        if (System.currentTimeMillis() % 25000 < 5000) {
//             generateRandomContent();
//        }
//    }
    /**
     * 新增方法：随机生成一个新的用户（MemberRecord）。
     */
    private MemberRecord generateRandomMember(String behaviorName, String domainTag) {
        // 获取递增的成员 ID
        long newMemberId = memberIdCounter.incrementAndGet();

        String newName = behaviorName + newMemberId + domainTag;

        // 设置加入时间在过去 GENERATE_DAYS_RANGE 天内
        LocalDateTime newJoinDate = LocalDateTime.now().minusDays(GENERATE_DAYS_RANGE);

        // 创建新成员
        MemberRecord newMember = new MemberRecord(newMemberId, newName, newJoinDate);
        memberDB.put(newMemberId, newMember);
        // 显示日志
        log.info("【新成员】注册成功: ID={}, 姓名={}, 加入时间={}", newMemberId, newName, newJoinDate);

        return newMember;
    }

//    private void generateRandomInteraction() {
//        if (contentDB.isEmpty() || memberDB.isEmpty()) return;
//
//        // 随机选择一个已发布内容
//        Long[] contentIds = contentDB.keySet().toArray(new Long[0]);
//        Long contentId = contentIds[random.nextInt(contentIds.length)];
//
//        // 随机选择一个成员
//        Long[] memberIds = memberDB.keySet().toArray(new Long[0]);
//        Long memberId = memberIds[random.nextInt(memberIds.length)];
//
//        // 随机选择一个互动类型 (6种类型)
//        InteractionType[] types = InteractionType.values();
//        InteractionType type = types[random.nextInt(types.length)];
//
//        // 1. 创建新事件 (用于评级系统批量拉取)
//        InteractionEventRecord newEvent = new InteractionEventRecord(
//                eventIdCounter.incrementAndGet(),
//                contentId,
//                memberId,
//                type,
//                LocalDateTime.now()
//        );
//
//        eventLog.add(newEvent);
//        log.debug("生成互动事件: 内容ID={}, 成员ID={}, 类型={}", contentId, memberId, type);
//
//        // 2. 【实时更新快照】更新 ContentRecord 中的累积计数
//        ContentRecord content = contentDB.get(contentId);
//        if (content != null) {
//            switch (type) {
//                case READ -> content.readCount().incrementAndGet();
//                case LIKE -> content.likeCount().incrementAndGet(); // 移除 checkAchievement() 调用
//                case COMMENT -> content.commentCount().incrementAndGet();
//                case SHARE -> content.shareCount().incrementAndGet();
//                case COLLECT -> content.collectCount().incrementAndGet();
//                case HATE -> content.hateCount().incrementAndGet();
//            }
//        }
//    }
    
//    private void generateRandomContent() {
//        Long newMemberId = memberDB.keySet().stream().skip(random.nextInt(memberDB.size())).findFirst().orElse(101L);
//        Long newContentId = contentIdCounter.incrementAndGet();
//
//        // 随机分配知识领域标签
//        String randomTag = KNOWLEDGE_AREA_TAGS.get(random.nextInt(KNOWLEDGE_AREA_TAGS.size()));
//
//        // 随机分配帖子长度等级 (1, 2, or 3)
//        int lengthLevel = random.nextInt(3) + 1;
//
//        ContentRecord newContent = new ContentRecord(
//                newContentId,
//                newMemberId,
//                "新内容标题-" + newContentId,
//                LocalDateTime.now(),
//                randomTag,
//                lengthLevel,
//                new AtomicLong(random.nextInt(10) + 1), // 初始阅读数
//                new AtomicLong(0),
//                new AtomicLong(0),
//                new AtomicLong(0),
//                new AtomicLong(0),
//                new AtomicLong(0)
//        );
//        contentDB.put(newContentId, newContent);
//        log.info("生成新内容: ID={}, 作者={}, 领域={}, 长度={}", newContentId, newMemberId, randomTag, lengthLevel);
//    }
    
    
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
        snapshot.put("read_count_snapshot", c.readCount);
        snapshot.put("like_count_snapshot", c.likeCount);
        snapshot.put("comment_count_snapshot", c.commentCount);
        snapshot.put("share_count_snapshot", c.shareCount);
        snapshot.put("collect_count_snapshot", c.collectCount);
        snapshot.put("hate_count_snapshot", c.hateCount);
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

    /**
     * 辅助方法：生成围绕基数的随机长整型数值。
     * @param base 基数
     * @param percentage 波动百分比
     * @return 随机长整型数值
     */
    private long randAround(long base, int percentage) {
        long round = (long) (base * percentage / 100.0);
        return random.nextLong(base - round, base + round + 1);
    }

    private int randAround(int base, int percentage) {
        int round = (int) (base * percentage / 100.0);
        return random.nextInt(base - round, base + round + 1);
    }

    private <T> List<T> sample(List<T> elements, int k) {
        if (k <= 0) return Collections.emptyList();
        int size = elements.size();
        if (size <= k) return new ArrayList<>(elements);

        return random.ints(0, size)
                .distinct().limit(k)
                .mapToObj(elements::get)
                .toList();
    }

    private List<LocalDateTime> sampleDates(LocalDateTime start, int days, int k) {
        if (k <= 0) return Collections.emptyList();
        LocalDateTime end = start.plusDays(days);
        long startEpochDay = start.toEpochSecond(ZoneOffset.UTC);
        long endEpochDay = end.toEpochSecond(ZoneOffset.UTC);

        return random.longs(startEpochDay, endEpochDay)
                .limit(k).sorted()
                .mapToObj(epoch -> LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC))
                .toList();
    }

    private void logContent(ContentRecord content) {
        log.info("内容ID: {}, 作者ID: {}, 领域: {}, 发布时间: {}, 阅读数: {}, 点赞数: {}, 评论数: {}, 分享数: {}, 收藏数: {}, 点踩数: {}",
                content.id, content.authorId, content.knowledgeTag, content.publishTime,
                content.readCount, content.likeCount, content.commentCount,
                content.shareCount, content.collectCount, content.hateCount);
    }

    // 辅助类：加权随机抽样
    @Getter
    private static class WeightedList<T> {
        List<T> elements;
        List<Integer> weights;
        int totalWeight;

        WeightedList(List<T> elements, List<Integer> weights) {
            assert elements.size() == weights.size();
            this.elements = elements;
            this.weights = weights;
            this.totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        }

        WeightedList(List<Pair<T, Integer>> pairs) {
            this.elements = pairs.stream().map(p->p.a).toList();
            this.weights = pairs.stream().map(p->p.b).toList();
            this.totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        }

        public int size() {
            return elements.size();
        }

        // 将num均分到各元素上，返回分布列表
        public int[] getDistributeList(int num) {
            int[] distributes = new int[this.size()];
            // 先分配可整除的部分
            int count = num / totalWeight;
            int remain = num % totalWeight;
            for (int i = 0; i < this.size(); i++) {
                distributes[i] = weights.get(i) * count;
            }
            // 然后分配剩余部分
            if (remain > 0) {
                // 之后再细化算法吧, 大致是需要计算小数, 然后考虑哪些向上取整哪些向下取整
            }

            return distributes;
        }

    }

    private double normalRandom(double mean, double stdDev) {
        return mean + random.nextGaussian() * stdDev;
    }

    private <T> T sample(WeightedList<T> weightedList) {
        int rand = random.nextInt(weightedList.getTotalWeight());
        int cumulativeWeight = 0;
        for (int i = 0; i < weightedList.size(); i++) {
            cumulativeWeight += weightedList.getWeights().get(i);
            if (rand < cumulativeWeight) {
                return weightedList.getElements().get(i);
            }
        }
        throw new IllegalStateException("WeightedList sampling failed");
    }
}