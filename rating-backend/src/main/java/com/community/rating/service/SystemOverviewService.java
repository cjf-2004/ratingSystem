// File: SystemOverviewService.java
package com.community.rating.service;

import com.community.rating.dto.SystemOverviewDTO;

/**
 * 系统概览数据的服务接口
 */
public interface SystemOverviewService {
    /**
     * 获取系统概览完整数据，整合来自多个数据源的统计信息。
     * @return 包含所有系统统计数据的 SystemOverviewDTO
     */
    SystemOverviewDTO getSystemOverview();
}