package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * COMMENT_EXPERT: 累计收到评论总数达到200
 */
@Component
public class CommentExpertRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;
    private static final long THRESHOLD = 200;

    public CommentExpertRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "COMMENT_EXPERT";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsByCumulativeCommentsGreaterThanEqual(THRESHOLD);
    }
}
