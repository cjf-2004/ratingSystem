package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.entity.Member;
import com.community.rating.repository.MemberRatingRepository;
import com.community.rating.repository.MemberRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FAST_GROWTH: 加入社群30天内，在任一领域达到L3评级（时间性）
 */
@Component
public class FastGrowthRule implements AchievementRule {

    private final MemberRepository memberRepo;
    private final MemberRatingRepository ratingRepo;

    public FastGrowthRule(MemberRepository memberRepo, MemberRatingRepository ratingRepo) {
        this.memberRepo = memberRepo;
        this.ratingRepo = ratingRepo;
    }

    @Override
    public String getAchievementKey() {
        return "FAST_GROWTH";
    }

    @Override
    public List<Long> detect() {
        LocalDateTime cutoff = LocalDateTime.now();
        // collect members who joined within last 30 days
        List<Member> recent = memberRepo.findAll().stream()
                .filter(m -> m.getJoinDate() != null && m.getJoinDate().isAfter(cutoff.minusDays(30)))
                .collect(Collectors.toList());

        List<Long> recentIds = recent.stream().map(Member::getMemberId).collect(Collectors.toList());

        return ratingRepo.findAll().stream()
                .filter(r -> recentIds.contains(r.getMemberId()) && r.getRatingLevel() != null && parseLevel(r.getRatingLevel()) >= 3)
                .map(r -> r.getMemberId())
                .distinct()
                .collect(Collectors.toList());
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
