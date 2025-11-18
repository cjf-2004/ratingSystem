package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.entity.MemberRating;
import com.community.rating.repository.MemberRatingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * VERSATILE_MEMBER: 在3个不同知识领域均达到L2及以上评级（复合性）
 */
@Component
public class VersatileMemberRule implements AchievementRule {

    private final MemberRatingRepository ratingRepo;

    public VersatileMemberRule(MemberRatingRepository ratingRepo) {
        this.ratingRepo = ratingRepo;
    }

    @Override
    public String getAchievementKey() {
        return "VERSATILE_MEMBER";
    }

    @Override
    public List<Long> detect() {
        List<MemberRating> all = ratingRepo.findAll();
        Map<Long, Set<Integer>> map = all.stream()
                .filter(r -> r.getRatingLevel() != null && parseLevel(r.getRatingLevel()) >= 2)
                .collect(Collectors.groupingBy(MemberRating::getMemberId, Collectors.mapping(MemberRating::getAreaId, Collectors.toSet())));

        return map.entrySet().stream().filter(e -> e.getValue().size() >= 3).map(e -> e.getKey()).collect(Collectors.toList());
    }

    private int parseLevel(String level) {
        if (level == null) return 0;
        try {
            if (level.startsWith("L") || level.startsWith("l")) {
                return Integer.parseInt(level.substring(1));
            }
            return Integer.parseInt(level);
        } catch (Exception ex) {
            return 0;
        }
    }
}
