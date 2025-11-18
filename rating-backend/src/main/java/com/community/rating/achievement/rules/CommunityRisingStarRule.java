package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommunityRisingStarRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;
    private static final long THRESHOLD = 500;

    public CommunityRisingStarRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "COMMUNITY_RISING_STAR";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsByCumulativeLikesGreaterThanEqual(THRESHOLD);
    }
}
