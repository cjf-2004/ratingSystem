package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.entity.Member;
import com.community.rating.repository.MemberRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成长里程碑类：用户成为社区老兵（加入时间超过 365 天）
 */
@Component
public class CommunityVeteranRule implements AchievementRule {

    private final MemberRepository memberRepo;
    private static final long DAYS_THRESHOLD = 365;

    public CommunityVeteranRule(MemberRepository memberRepo) {
        this.memberRepo = memberRepo;
    }

    @Override
    public String getAchievementKey() {
        return "COMMUNITY_VETERAN";
    }

    @Override
    public List<Long> detect() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(DAYS_THRESHOLD);
        return memberRepo.findAll().stream()
                .filter(m -> m.getJoinDate() != null && m.getJoinDate().isBefore(cutoff))
                .map(Member::getMemberId)
                .collect(Collectors.toList());
    }
}
