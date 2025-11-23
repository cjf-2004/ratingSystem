// com.community.rating.controller.AchievementController.java
package com.community.rating.controller;

import com.community.rating.dto.AchievementDTO;
import com.community.rating.dto.CommonResponse; // 引入 CommonResponse
import com.community.rating.service.AchievementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/Achievement")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    /**
     * **路径: /api/Achievement/getAchievementList**
     * 功能: 获取所有成就的定义信息清单
     */
    @GetMapping("/getAchievementList")
    public ResponseEntity<CommonResponse<List<AchievementDTO>>> getAchievementList() {
        List<AchievementDTO> list = achievementService.getAchievementList();
        
        if (list.isEmpty()) {
            // 如果列表为空，返回 HTTP 200，但在 CommonResponse 中返回空数据或 success()
            // 采用 success(List) 保持数据类型一致性
            return ResponseEntity.ok(CommonResponse.success(list)); 
        }
        
        // 成功返回 200 OK，数据体为 CommonResponse
        return ResponseEntity.ok(CommonResponse.success(list));
    }

    /**
     * **路径: /api/Achievement/getAchievementRanking?count=5&sort_order=desc**
     * 功能: 获取成就排名列表，按达成人数 (achieved_count) 排序
     * @param count 结果数量 (可选, 默认 1)
     * @param sortOrder 排序顺序 ("asc"/"desc", 可选, 默认 "desc")
     */
    @GetMapping("/getAchievementRanking")
    public ResponseEntity<CommonResponse<List<AchievementDTO>>> getAchievementRanking(
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort_order", required = false) String sortOrder) {

        List<AchievementDTO> ranking = achievementService.getAchievementRanking(count, sortOrder);

        if (ranking.isEmpty()) {
            // 如果列表为空，返回 HTTP 200，CommonResponse data 为空列表
            return ResponseEntity.ok(CommonResponse.success(ranking));
        }
        
        // 成功返回 200 OK
        return ResponseEntity.ok(CommonResponse.success(ranking));
    }

}