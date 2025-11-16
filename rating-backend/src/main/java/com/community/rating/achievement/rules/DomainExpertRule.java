package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.repository.MemberRatingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DOMAIN_EXPERT: 在任一知识领域达到L5最高评级（一次性）
 */
@Component
public class DomainExpertRule implements AchievementRule {

    private final MemberRatingRepository ratingRepo;

    public DomainExpertRule(MemberRatingRepository ratingRepo) {
        this.ratingRepo = ratingRepo;
    }

    @Override
    public String getAchievementKey() {
        return "DOMAIN_EXPERT";
    }

    @Override
    public List<Long> detect() {
        return ratingRepo.findAll().stream()
                .filter(r -> r.getRatingLevel() != null && ("L5".equalsIgnoreCase(r.getRatingLevel()) || "5".equals(r.getRatingLevel())))
                .map(r -> r.getMemberId())
                .distinct()
                .collect(Collectors.toList());
    }
}
