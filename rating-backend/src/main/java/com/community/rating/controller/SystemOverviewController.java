package com.community.rating.controller;

import com.community.rating.dto.CommonResponse;
import com.community.rating.dto.SystemOverviewDTO;
import com.community.rating.service.SystemOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor // Lombok: 注入 Service
public class SystemOverviewController {

    private final SystemOverviewService systemOverviewService;

    /**
     * GET /api/system-overview
     * 获取系统概览看板数据，包括统计数字和排行榜。
     *
     * @return 封装在 CommonResponse 中的 SystemOverviewDTO
     */
    @GetMapping("/SystemOverview")
    public ResponseEntity<CommonResponse<SystemOverviewDTO>> getSystemOverview() {
        
        // 1. 调用 Service 层获取业务数据
        SystemOverviewDTO overviewData = systemOverviewService.getSystemOverview();
        
        // 2. 使用 CommonResponse 静态方法封装成功响应 (code: 200)
        CommonResponse<SystemOverviewDTO> response = CommonResponse.success(overviewData);
        
        // 3. 返回 HTTP 200 OK
        return ResponseEntity.ok(response);
    }
}