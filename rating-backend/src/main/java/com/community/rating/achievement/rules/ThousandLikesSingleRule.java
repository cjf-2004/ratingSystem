package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * THOUSAND_LIKES_SINGLE: 单条内容获得的点赞数达到1000
 */
@Component
public class ThousandLikesSingleRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;

    public ThousandLikesSingleRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "THOUSAND_LIKES_SINGLE";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsWithAnyPostLikeAtLeast(1000);
    }
}
