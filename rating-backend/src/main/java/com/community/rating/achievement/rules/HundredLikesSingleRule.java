package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HundredLikesSingleRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;

    public HundredLikesSingleRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "HUNDRED_LIKES_SINGLE";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsWithAnyPostLikeAtLeast(100);
    }
}
