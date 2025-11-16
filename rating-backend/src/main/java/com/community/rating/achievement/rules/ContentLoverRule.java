package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CONTENT_LOVER: 累计发布内容达到10条
 */
@Component
public class ContentLoverRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;
    private static final long THRESHOLD = 10;

    public ContentLoverRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "CONTENT_LOVER";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsByPostCountGreaterThanEqual(THRESHOLD);
    }
}
