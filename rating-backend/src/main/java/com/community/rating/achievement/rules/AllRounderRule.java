package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.entity.MemberRating;
import com.community.rating.repository.MemberRatingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 综合贡献类：在至少 3 个不同领域达到等级 L3 或以上（allrounder）
 */
@Component
public class AllRounderRule implements AchievementRule {

    private final MemberRatingRepository ratingRepo;

    public AllRounderRule(MemberRatingRepository ratingRepo) {
        this.ratingRepo = ratingRepo;
    }

    @Override
    public String getAchievementKey() {
        return "ALL_ROUND_CONTRIBUTOR";
    }

    @Override
    public List<Long> detect() {
        List<MemberRating> all = ratingRepo.findAll();
        Map<Long, Long> memberAreaQualified = all.stream()
                .filter(r -> r.getRatingLevel() != null && parseLevel(r.getRatingLevel()) >= 3)
                .collect(Collectors.groupingBy(MemberRating::getMemberId, Collectors.mapping(MemberRating::getAreaId, Collectors.toSet())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        return memberAreaQualified.entrySet().stream().filter(e -> e.getValue() >= 3).map(e -> e.getKey()).collect(Collectors.toList());
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
