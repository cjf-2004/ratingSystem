package com.community.rating.simulation;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 单文件时间模拟服务：支持真实/虚拟时间切换，虚拟时间每 5 分钟推进 1 天并持久化。
 */
public class TimeSimulation {

    // 全局实例
    private static final VirtualTimeProvider VIRTUAL_TIME_PROVIDER = new VirtualTimeProvider();
    // 时间模式
    private static final boolean IS_REAL_TIME = false;
    // 获取当前时间
    public static LocalDateTime now() {
        return IS_REAL_TIME ? LocalDateTime.now() : VIRTUAL_TIME_PROVIDER.now();
    }

    // ---- 虚拟时间 ----
    private static class VirtualTimeProvider {
        private static final String STORE_FOLDER = "./simulation";
        private static final String STORE_FILE = "virtual_time.txt";

        private static final int SPEED_MULTIPLIER = 60 * 60 * 24 / 30; // 30 秒推进 1 天

        private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        private static final Path STORE_DIR = Paths.get(STORE_FOLDER);
        private static final Path STORE = STORE_DIR.resolve(STORE_FILE);

        private final Object lock = new Object();
        private LocalDateTime current;
        private Instant lastRealTick;
        private Instant lastPersistAt;

        VirtualTimeProvider() {
            this.current = loadFromFile().orElse(LocalDateTime.now());
            this.lastRealTick = Instant.now();
            this.lastPersistAt = this.lastRealTick;

            // 每 1 秒按 288 倍推进虚拟时间（小时/分钟会连续更新）
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "virtual-time-scheduler");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
        }

        private void tick() {
            try {
                Instant nowReal = Instant.now();
                long realMillis = Duration.between(lastRealTick, nowReal).toMillis();
                if (realMillis <= 0) {
                    lastRealTick = nowReal;
                    return;
                }
                lastRealTick = nowReal;

                // 放大真实时间间隔
                long virtualMillis = realMillis * SPEED_MULTIPLIER;

                synchronized (lock) {
                    current = current.plusNanos(virtualMillis * 1_000_000L);
                    // 每 10 秒持久化一次，避免过于频繁 I/O
                    if (Duration.between(lastPersistAt, nowReal).getSeconds() >= 10) {
                        persistToFile();
                        lastPersistAt = nowReal;
                    }
                }
            } catch (Exception e) {
                System.err.println("Virtual time tick failed: " + e.getMessage());
            }
        }

        public LocalDateTime now() {
            synchronized (lock) {
                return current;
            }
        }

        private void persistToFile() throws IOException {
            if (Files.notExists(STORE_DIR)) {
                Files.createDirectories(STORE_DIR);
            }
            Files.writeString(STORE, current.format(FMT),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
    }
}
