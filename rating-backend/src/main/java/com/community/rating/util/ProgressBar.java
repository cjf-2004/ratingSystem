package com.community.rating.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 通用进度条工具类
 * 用于在长时间运行的任务中显示进度信息
 */
public class ProgressBar {
    private static final Logger log = LoggerFactory.getLogger(ProgressBar.class);
    private final String taskName;
    private final int totalSteps;
    private int currentStep;
    private final LocalDateTime startTime;
    private final int updateInterval;
    private int lastReportedPercentage = -1;
    private boolean isFirstUpdate = true;

    /**
     * 构造函数
     * @param taskName 任务名称
     * @param totalSteps 总步骤数
     */
    public ProgressBar(String taskName, int totalSteps) {
        this(taskName, totalSteps, 5); // 默认每5%更新一次
    }

    /**
     * 构造函数
     * @param taskName 任务名称
     * @param totalSteps 总步骤数
     * @param updateInterval 更新间隔（百分比）
     */
    public ProgressBar(String taskName, int totalSteps, int updateInterval) {
        this.taskName = taskName;
        this.totalSteps = totalSteps > 0 ? totalSteps : 1;
        this.currentStep = 0;
        this.startTime = LocalDateTime.now();
        this.updateInterval = updateInterval;
        // 开始任务时输出一行，后续更新使用同一行
        System.out.printf("[%s] 开始任务，共 %d 步\n", taskName, totalSteps);
    }

    /**
     * 步进一次
     */
    public void step() {
        increment(1);
    }

    /**
     * 步进指定次数
     * @param steps 步进次数
     */
    public void increment(int steps) {
        currentStep += steps;
        displayProgress();
    }

    /**
     * 直接设置当前进度
     * @param currentStep 当前步骤
     */
    public void setCurrentStep(int currentStep) {
        this.currentStep = Math.min(currentStep, totalSteps);
        displayProgress();
    }

    /**
     * 完成任务
     */
    public void complete() {
        currentStep = totalSteps;
        // 完成时强制输出最后状态，并换行
        String estimatedTime = calculateEstimatedTime(100);
        System.out.printf("\r[%s] 进度: %d/%d (100)%% | 预计剩余: 0s\n", 
                taskName, currentStep, totalSteps);
        
        long duration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
        System.out.printf("[%s] 任务完成！总耗时: %dms\n", taskName, duration);
    }

    /**
     * 显示进度
     */
    private void displayProgress() {
        int percentage = calculatePercentage();
        
        // 只有当百分比变化且达到更新间隔时才显示
        if (percentage != lastReportedPercentage && (percentage % updateInterval == 0 || percentage == 100)) {
            String estimatedTime = calculateEstimatedTime(percentage);
            // 使用\r回车符回到行首，覆盖之前的输出
            System.out.printf("\r[%s] 进度: %d/%d (%d)%% | 预计剩余: %s", 
                    taskName, currentStep, totalSteps, percentage, estimatedTime);
            lastReportedPercentage = percentage;
        }
    }

    /**
     * 计算完成百分比
     * @return 百分比
     */
    private int calculatePercentage() {
        return Math.min(100, (int) ((currentStep * 100.0) / totalSteps));
    }

    /**
     * 计算预计剩余时间
     * @param percentage 当前百分比
     * @return 格式化的剩余时间
     */
    private String calculateEstimatedTime(int percentage) {
        if (percentage == 0) {
            return "计算中...";
        }
        
        long elapsedMs = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
        long totalEstimatedMs = (elapsedMs * 100) / percentage;
        long remainingMs = totalEstimatedMs - elapsedMs;
        
        if (remainingMs < 1000) {
            return "<1s";
        } else if (remainingMs < 60000) {
            return (remainingMs / 1000) + "s";
        } else {
            return (remainingMs / 60000) + "m " + ((remainingMs % 60000) / 1000) + "s";
        }
    }
}