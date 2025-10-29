package com.community.rating.service;

import com.community.rating.entity.MemberRating;
import com.community.rating.repository.MemberRatingRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RatingService {

    private final MemberRatingRepository ratingRepository;

    public RatingService(MemberRatingRepository ratingRepository) {
        // 依赖注入 MemberRatingRepository
        this.ratingRepository = ratingRepository;
    }

    /**
     * 获取指定领域的影响力排行榜 (R4.1)
     * @param areaId 知识领域 ID
     * @return 评级列表（已排序）
     */
    public List<MemberRating> getTopRatingsByArea(Integer areaId) {
        return ratingRepository.findByKnowledgeAreaIdOrderByDesScoreDesc(areaId);
    }

    /**
     * 获取特定成员的所有领域评级详情 (R4.2)
     * @param memberId 成员 ID
     * @return 成员的所有评级记录
     */
    public List<MemberRating> getMemberAllRatings(Long memberId) {
        return ratingRepository.findByMemberId(memberId);
    }
    
    // TODO: 实现成就状态（AchievementStatus）和每日评级计算的 Service 方法
}
