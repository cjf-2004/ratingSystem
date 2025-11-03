package com.community.rating.simulation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 【模拟数据接口】用于可视化和测试 ForumDataSimulation 的数据拉取（PULL）功能。
 * 评级系统开发人员可以通过浏览器直接访问这些接口，实时查看模拟数据。
 */
@RestController
@RequestMapping("/api/simulation")
public class ForumSimulationController {

    private static final Logger log = LoggerFactory.getLogger(ForumSimulationController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final ForumDataSimulation forumDataSimulation;

    @Autowired
    public ForumSimulationController(ForumDataSimulation forumDataSimulation) {
        this.forumDataSimulation = forumDataSimulation;
    }

    /**
     * GET /api/simulation/member-snapshot
     * 接口：获取全量成员数据快照。
     * @return 成员列表
     */
    @GetMapping("/member-snapshot")
    public List<Map<String, Object>> getMemberSnapshot() {
        log.info("API CALL: 拉取成员快照。");
        return forumDataSimulation.getMemberSnapshot();
    }

    /**
     * GET /api/simulation/content-snapshot
     * 接口：获取全量内容数据快照 (包含实时计数器)。
     * @return 内容快照列表
     */
    @GetMapping("/content-snapshot")
    public List<Map<String, Object>> getContentSnapshot() {
        log.info("API CALL: 拉取内容快照。");
        return forumDataSimulation.getContentSnapshot();
    }

    /**
     * GET /api/simulation/interaction-events
     * 接口：获取指定时间窗口内的互动事件记录。
     * 默认拉取过去 30 秒的事件。
     * @param startTime 开始时间 (格式: YYYY-MM-DDTHH:MM:SS)
     * @param endTime 结束时间 (格式: YYYY-MM-DDTHH:MM:SS)
     * @return 互动事件列表
     */
    @GetMapping("/interaction-events")
    public List<Map<String, Object>> getAllInteractionEventsInWindow(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        LocalDateTime end = (endTime != null) ? LocalDateTime.parse(endTime, FORMATTER) : LocalDateTime.now();
        LocalDateTime start = (startTime != null) ? LocalDateTime.parse(startTime, FORMATTER) : end.minusSeconds(30);

        log.info("API CALL: 拉取互动事件，时间窗口从 {} 到 {}。", start, end);
        
        return forumDataSimulation.getAllInteractionEventsInWindow(start, end);
    }
}
