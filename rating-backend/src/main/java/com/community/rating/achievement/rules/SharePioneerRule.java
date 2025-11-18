package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SHARE_PIONEER: 累计被转发总数达到100
 */
@Component
public class SharePioneerRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;
    private static final long THRESHOLD = 100;

    public SharePioneerRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "SHARE_PIONEER";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsByCumulativeSharesGreaterThanEqual(THRESHOLD);
    }
}
