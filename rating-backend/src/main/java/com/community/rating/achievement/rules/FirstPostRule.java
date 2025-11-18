package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 内容创作类：首次发布作品（first_post）
 */
@Component
public class FirstPostRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;

    public FirstPostRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "FIRST_POST";
    }

    @Override
    public List<Long> detect() {
        // 从内容表中获取所有已存在的作者 ID（去重）
        return contentRepo.findAll().stream()
                .map(c -> c.getMemberId())
                .distinct()
                .collect(Collectors.toList());
    }
}
