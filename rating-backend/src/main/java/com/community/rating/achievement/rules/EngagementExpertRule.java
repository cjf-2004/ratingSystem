package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ENGAGEMENT_EXPERT: 单条内容总互动量（点赞+评论+转发）达到200
 */
@Component
public class EngagementExpertRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;

    public EngagementExpertRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "ENGAGEMENT_EXPERT";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsWithAnyPostEngagementAtLeast(200);
    }
}
