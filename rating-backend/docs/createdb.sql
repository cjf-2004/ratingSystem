-- ----------------------------------------------------
-- 数据库和编码设置
-- ----------------------------------------------------
CREATE DATABASE IF NOT EXISTS RatingDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE RatingDB;

-- 设置存储引擎为 InnoDB 以支持事务和外键
SET default_storage_engine=InnoDB;

-- ----------------------------------------------------
-- 1. 基础信息表
-- ----------------------------------------------------

-- 1.1 Member (成员信息表)
CREATE TABLE Member (
    member_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '成员唯一标识，来自数据源',
    name VARCHAR(100) NOT NULL COMMENT '成员昵称',
    join_date TIMESTAMP NOT NULL COMMENT '加入社群时间'
) COMMENT='存储成员基础信息';


-- 1.2 KnowledgeArea (知识领域配置表)
CREATE TABLE KnowledgeArea (
    area_id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '领域唯一 ID',
    area_name VARCHAR(50) NOT NULL UNIQUE COMMENT '一级领域名称',
    sub_tags_list TEXT COMMENT '存储二级标签示例，用于前端展示'
) COMMENT='存储领域配置和标签';

INSERT INTO KnowledgeArea (area_name, sub_tags_list) 
VALUES
('前端开发', 'HTML/CSS、JavaScript、React、Vue'),
('后端开发', 'Java、Python、Go、数据库、服务器'),
('数据科学', '数据分析、机器学习、数据可视化、算法'),
('视觉设计', 'UI 设计、交互设计、动效设计、品牌设计'),
('产品管理', '需求分析、产品规划、用户研究、项目管理'),
('云计算与运维', '云平台、容器技术、网络安全、自动化运维');

-- 1.3 AchievementDefinition (成就定义表)
CREATE TABLE AchievementDefinition (
    achievement_key VARCHAR(50) NOT NULL PRIMARY KEY COMMENT '成就的唯一KEY（如：FIRST_POST）',
    name VARCHAR(100) NOT NULL COMMENT '成就的展示名称',
    type VARCHAR(20) NOT NULL COMMENT '成就类型（一次性、累计性等）',
    trigger_condition_desc TEXT NOT NULL COMMENT '成就触发条件的详细描述'
) COMMENT='存储成就的静态定义和规则';

INSERT INTO AchievementDefinition (achievement_key, name, type, trigger_condition_desc) 
VALUES
-- A. 内容创作类成就
('FIRST_POST', '初试锋芒', '一次性', '发布第一条内容'),
('CONTENT_LOVER', '内容爱好者', '累计性', '累计发布内容达到10条'),
('CONTENT_MASTER', '内容大师', '累计性', '累计发布内容达到100条'),
('PROLIFIC_AUTHOR', '高产作者', '一次性', '单日发布内容达到5条'),
('CONSISTENT_CREATOR', '持续创作者', '连续性', '连续7天每天发布至少1条内容'),

-- B. 互动影响力类成就
('HUNDRED_LIKES_SINGLE', '百赞作者', '一次性', '单条内容获得的点赞数达到100'),
('THOUSAND_LIKES_SINGLE', '千赞作者', '一次性', '单条内容获得的点赞数达到1000'),
('POPULAR_AUTHOR', '热门作者', '一次性', '单条内容的评论数达到50条'),
('CONTENT_SHARER', '内容传播者', '一次性', '单条内容被转发达到30次'),
('ENGAGEMENT_EXPERT', '互动达人', '一次性', '单条内容总互动量（点赞+评论+转发）达到200'),

-- C. 综合贡献类成就
('COMMUNITY_RISING_STAR', '社群新星', '累计性', '累计获得点赞总数达到500'),
('COMMUNITY_STAR', '社群明星', '累计性', '累计获得点赞总数达到5000'),
('COMMENT_EXPERT', '评论专家', '累计性', '累计收到评论总数达到200'),
('SHARE_PIONEER', '转发先锋', '累计性', '累计被转发总数达到100'),
('ALL_ROUND_CONTRIBUTOR', '全能贡献者', '复合性', '在3个不同知识领域均达到L3评级'),

-- D. 成长里程碑类成就
('DOMAIN_EXPERT', '领域专家', '一次性', '在任一知识领域达到L5最高评级'),
('VERSATILE_MEMBER', '多面手', '复合性', '在3个不同知识领域均达到L2及以上评级'),
('FAST_GROWTH', '快速成长', '时间性', '加入社群30天内，在任一领域达到L3评级'),
('COMMUNITY_VETERAN', '社群元老', '时间性', '加入社群满1年且持续活跃');


-- ----------------------------------------------------
-- 2. 核心数据与计算结果表
-- ----------------------------------------------------

-- 2.1 ContentSnapshot (内容快照表)
CREATE TABLE ContentSnapshot (
    content_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '帖子唯一 ID',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '内容作者 ID',
    publish_time TIMESTAMP NOT NULL COMMENT '帖子发布时间，用于时效性计算',
    area_id INT UNSIGNED NOT NULL COMMENT '帖子所属领域id',
    post_length_level TINYINT UNSIGNED NOT NULL COMMENT '帖子长度分级 (1, 2, 3)',
    
    -- 快照数据（CIS输入，确保非空）
    read_count_snapshot INT UNSIGNED NOT NULL,
    like_count_snapshot INT UNSIGNED NOT NULL,
    comment_count_snapshot INT UNSIGNED NOT NULL,
    share_count_snapshot INT UNSIGNED NOT NULL,
    collect_count_snapshot INT UNSIGNED NOT NULL,
    hate_count_snapshot INT UNSIGNED NOT NULL,
    
    -- CIS 计算结果（核心输出）
    cis_score DECIMAL(10, 4) NOT NULL COMMENT '内容影响力分数',
    
    -- 外键和索引
    FOREIGN KEY (member_id) REFERENCES Member(member_id),
    FOREIGN KEY (area_id) REFERENCES KnowledgeArea(area_id),
    -- 索引：支持批量计算时的聚合和时间过滤
    INDEX idx_member_tag (member_id, area_id),
    INDEX idx_publish_time (publish_time),
    -- 性能优化索引：支持成就规则检测的快速查询
    INDEX idx_like_count (like_count_snapshot),
    INDEX idx_comment_count (comment_count_snapshot),
    INDEX idx_share_count (share_count_snapshot)
) COMMENT='每日批量计算的原始数据快照及CIS得分';


-- 2.2 MemberRating (成员评级历史表)
CREATE TABLE MemberRating (
    rating_id BIGINT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    area_id INT UNSIGNED NOT NULL,
    
    des_score DECIMAL(12, 4) NOT NULL COMMENT 'DES_K 最终影响力分数',
    rating_level VARCHAR(2) NOT NULL COMMENT '映射后的等级 (L1-L5)',
    update_date DATE NOT NULL COMMENT '记录本次评级的计算日期',
    
    -- 外键约束
    FOREIGN KEY (member_id) REFERENCES Member(member_id),
    FOREIGN KEY (area_id) REFERENCES KnowledgeArea(area_id),
    
    -- 复合索引：支持快速查询成员在特定领域最新的评级记录（按日期倒序取第一条）
    INDEX idx_member_area_date (member_id, area_id, update_date DESC),
    
    -- 复合索引：支持按领域进行影响力分数排名（Web展示，需要子查询获取最新记录）
    INDEX idx_rank_des (area_id, des_score)
) COMMENT='存储成员在各领域的评级历史记录';


-- 2.3 AchievementStatus (成就状态表)
CREATE TABLE AchievementStatus (
    status_id BIGINT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    achievement_key VARCHAR(50) NOT NULL,
    
    achieved_time TIMESTAMP NOT NULL COMMENT '成就达成的精确时间',
    -- 外键约束
    FOREIGN KEY (member_id) REFERENCES Member(member_id),
    FOREIGN KEY (achievement_key) REFERENCES AchievementDefinition(achievement_key),
    
    -- 复合唯一索引：防止重复记录已达成的一次性成就
    UNIQUE KEY uk_member_achievement (member_id, achievement_key),
    
    -- 索引：快速查询成员成就列表
    INDEX idx_member_time (member_id, achieved_time),
    
    -- 性能优化索引：支持成就检测中的批量存在性检查
    -- 查询模式：WHERE achievement_key = ? AND member_id IN (...)
    INDEX idx_achievement_key_member (achievement_key, member_id)
) COMMENT='存储成员的成就达成状态和时间';

