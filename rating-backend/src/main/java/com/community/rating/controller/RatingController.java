package com.community.rating.controller;

import com.community.rating.entity.MemberRating;
import com.community.rating.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    /**
     * GET /api/v1/ratings/area/{areaId}
     * 功能: 获取指定领域的成员评级排行榜（按 DES_K 分数降序）
     * 对应需求: R4.1
     */
    @GetMapping("/area/{areaId}")
    public ResponseEntity<List<MemberRating>> getAreaLeaderboard(@PathVariable Integer areaId) {
        List<MemberRating> ratings = ratingService.getTopRatingsByArea(areaId);
        if (ratings.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        return ResponseEntity.ok(ratings); // 200 OK
    }

    /**
     * GET /api/v1/ratings/member/{memberId}
     * 功能: 查询指定成员的所有领域评级详情
     * 对应需求: R4.2
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<MemberRating>> getMemberRatings(@PathVariable Long memberId) {
        List<MemberRating> ratings = ratingService.getMemberAllRatings(memberId);
        if (ratings.isEmpty()) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
        return ResponseEntity.ok(ratings);
    }
}
