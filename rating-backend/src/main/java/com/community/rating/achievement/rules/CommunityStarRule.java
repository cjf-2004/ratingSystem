package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * COMMUNITY_STAR: 累计获得点赞总数达到5000
 */
@Component
public class CommunityStarRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;
    private static final long THRESHOLD = 5000;

    public CommunityStarRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "COMMUNITY_STAR";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsByCumulativeLikesGreaterThanEqual(THRESHOLD);
    }
}
