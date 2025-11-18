package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CONTENT_MASTER: 累计发布内容达到100条
 */
@Component
public class ContentMasterRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;
    private static final long THRESHOLD = 100;

    public ContentMasterRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "CONTENT_MASTER";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsByPostCountGreaterThanEqual(THRESHOLD);
    }
}
