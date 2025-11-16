package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * POPULAR_AUTHOR: 单条内容的评论数达到50条
 */
@Component
public class PopularAuthorRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;

    public PopularAuthorRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "POPULAR_AUTHOR";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsWithAnyPostCommentAtLeast(50);
    }
}
