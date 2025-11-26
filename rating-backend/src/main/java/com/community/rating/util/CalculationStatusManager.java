package com.community.rating.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 计算状态管理器 - 单例模式管理评分计算状态
 * 作为全局状态的中央管理点，避免直接在服务间共享状态
 */
@Component
public class CalculationStatusManager {
    
    private static final Logger log = LoggerFactory.getLogger(CalculationStatusManager.class);
    
    // 使用volatile确保多线程可见性
    private volatile boolean isCalculationInProgress = false;
    
    /**
     * 设置计算状态
     */
    public void setCalculationInProgress(boolean inProgress) {
        this.isCalculationInProgress = inProgress;
        log.info("评分计算状态已更新为: {}", inProgress ? "进行中" : "已完成");
    }
    
    /**
     * 获取计算状态
     */
    public boolean isCalculationInProgress() {
        return isCalculationInProgress;
    }
}