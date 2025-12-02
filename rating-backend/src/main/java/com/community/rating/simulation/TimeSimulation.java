package com.community.rating.simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 单文件时间模拟服务：支持真实/虚拟时间切换，虚拟时间每 30s 推进 1 天并持久化。
 */
public class TimeSimulation {

    // 全局实例
    private static final VirtualTimeProvider VIRTUAL_TIME_PROVIDER = new VirtualTimeProvider();
    // 时间模式
    private static final boolean IS_REAL_TIME = !ForumDataSimulation.USE_VIRTUAL_TIME;
    private static final boolean LOAD_FROM_FILE = ForumDataSimulation.IS_LOAD_MODE;
    // 获取当前时间
    public static LocalDateTime now() {
        return IS_REAL_TIME ? LocalDateTime.now() : VIRTUAL_TIME_PROVIDER.now();
    }

    // ---- 虚拟时间 ----
    public static class VirtualTimeProvider {
        private static final String STORE_FOLDER = "./simulation";
        private static final String STORE_FILE = "virtual_time.txt";

        public static final int SECONDS_OF_SIMULATION_DAY = 20;
        private static final int SPEED_MULTIPLIER = 60 * 24 * 60 / SECONDS_OF_SIMULATION_DAY; // 120 秒推进 1 天

        private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        private static final Path STORE_DIR = Paths.get(STORE_FOLDER);
        private static final Path STORE = STORE_DIR.resolve(STORE_FILE);

        private final LocalDateTime startRealTime;
        private final LocalDateTime startVirtualTime;

        static {
            // 确保存储目录存在
            try {
                if (!Files.exists(STORE_DIR)) {
                    Files.createDirectories(STORE_DIR);
                }
            } catch (IOException e) {
                System.err.println("创建目录失败: " + e.getMessage());
            }
        }

        VirtualTimeProvider() {
            this.startRealTime = LocalDateTime.now();
            this.startVirtualTime = LOAD_FROM_FILE
                    ? loadFromFile().orElse(this.startRealTime)
                    : this.startRealTime;
            // 注册 JVM 关闭钩子，保存虚拟时间
            Runtime.getRuntime().addShutdownHook(new Thread(this::saveToFile));
        }

        public LocalDateTime now() {
            LocalDateTime now = LocalDateTime.now();
            Duration realElapsed = Duration.between(startRealTime, now);
            long virtualSeconds = realElapsed.getSeconds() * SPEED_MULTIPLIER;
            return startVirtualTime.plusSeconds(virtualSeconds);
        }

        private Optional<LocalDateTime> loadFromFile() {
            try {
                if (Files.exists(STORE)) {
                    String s = Files.readString(STORE).trim();
                    if (!s.isEmpty()) {
                        return Optional.of(LocalDateTime.parse(s, FMT));
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load virtual time: " + e.getMessage());
            }
            return Optional.empty();
        }

        private void saveToFile() {
            try {
                String s = now().format(FMT);
                Files.writeString(STORE, s, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("保存虚拟时间成功: " + s);
            } catch (Exception e) {
                System.err.println("Failed to save virtual time: " + e.getMessage());
            }
        }
    }
}
