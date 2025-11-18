package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PROLIFIC_AUTHOR: 单日发布内容达到5条（一次性）
 */
@Component
public class ProlificAuthorRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;
    private static final long THRESHOLD = 5;

    public ProlificAuthorRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "PROLIFIC_AUTHOR";
    }

    @Override
    public List<Long> detect() {
        // group by memberId and publish date, then check any date count >= THRESHOLD
        return contentRepo.findMemberIdsWithAnyDayPostCountAtLeast((int)THRESHOLD);
    }
}
