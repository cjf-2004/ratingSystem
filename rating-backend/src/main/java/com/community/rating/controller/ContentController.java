// com.community.rating.controller.ContentController.java
package com.community.rating.controller;

import com.community.rating.dto.CommonResponse;
import com.community.rating.dto.ContentDTO;
import com.community.rating.service.ContentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/Content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * **路径: /api/Content/getContentRanking**
     * 功能: 获取内容排名列表，按内容分数 (cis_score) 排序
     */
    @GetMapping("/getContentRanking")
    public ResponseEntity<CommonResponse<List<ContentDTO>>> getContentRanking(
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort_order", required = false) String sortOrder) {

        List<ContentDTO> ranking = contentService.getContentRanking(count, sortOrder);

        return ResponseEntity.ok(CommonResponse.success(ranking));
    }

    /**
     * **路径: /api/Content/searchContent**
     * 功能: 按帖子 ID 获取单个内容详情
     * @param contentId 帖子 ID (必需)
     */
    @GetMapping("/searchContent")
    public ResponseEntity<CommonResponse<ContentDTO>> searchContent(
            @RequestParam("content_id") Long contentId) {
        
        ContentDTO content = contentService.getContentById(contentId);

        if (content == null) {
            return ResponseEntity.ok(CommonResponse.success(null));
        }
        
        return ResponseEntity.ok(CommonResponse.success(content));
    }
}