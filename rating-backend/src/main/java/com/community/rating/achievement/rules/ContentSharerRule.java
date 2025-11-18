package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CONTENT_SHARER: 单条内容被转发达到30次
 */
@Component
public class ContentSharerRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;

    public ContentSharerRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "CONTENT_SHARER";
    }

    @Override
    public List<Long> detect() {
        return contentRepo.findMemberIdsWithAnyPostShareAtLeast(30);
    }
}
