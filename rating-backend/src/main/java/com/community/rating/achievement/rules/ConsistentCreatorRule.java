package com.community.rating.achievement.rules;

import com.community.rating.achievement.AchievementRule;
import com.community.rating.entity.ContentSnapshot;
import com.community.rating.repository.ContentSnapshotRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * CONSISTENT_CREATOR: 连续7天每天发布至少1条内容（连续性）
 */
@Component
public class ConsistentCreatorRule implements AchievementRule {

    private final ContentSnapshotRepository contentRepo;
    private static final int WINDOW_DAYS = 7;

    public ConsistentCreatorRule(ContentSnapshotRepository contentRepo) {
        this.contentRepo = contentRepo;
    }

    @Override
    public String getAchievementKey() {
        return "CONSISTENT_CREATOR";
    }

    @Override
    public List<Long> detect() {
        // We want ANY consecutive WINDOW_DAYS window (not necessarily ending today).
        // Approach: for each member collect the set of distinct publish dates, then
        // check whether there exists a start date d such that all d..d+WINDOW_DAYS-1 are present.

        List<ContentSnapshot> all = contentRepo.findAll();

        Map<Long, Set<LocalDate>> memberDates = all.stream()
                .filter(c -> c.getPublishTime() != null && c.getMemberId() != null)
                .collect(Collectors.groupingBy(ContentSnapshot::getMemberId,
                        Collectors.mapping(c -> c.getPublishTime().toLocalDate(), Collectors.toSet())));

        List<Long> result = new ArrayList<>();

        for (Map.Entry<Long, Set<LocalDate>> e : memberDates.entrySet()) {
            Set<LocalDate> dates = e.getValue();
            if (dates.size() < WINDOW_DAYS) continue;

            // iterate over sorted dates as potential window starts
            List<LocalDate> sorted = new ArrayList<>(dates);
            Collections.sort(sorted);

            boolean qualified = false;
            for (LocalDate start : sorted) {
                boolean ok = true;
                for (int i = 0; i < WINDOW_DAYS; i++) {
                    if (!dates.contains(start.plusDays(i))) { ok = false; break; }
                }
                if (ok) { qualified = true; break; }
            }

            if (qualified) result.add(e.getKey());
        }

        return result;
    }
}
