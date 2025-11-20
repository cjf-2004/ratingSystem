package com.community.rating.simulation;

import java.io.IOException;
import java.nio.file.*;
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
    private static final boolean IS_REAL_TIME = true;
    // 获取当前时间
    public static LocalDateTime now() {
        return IS_REAL_TIME ? LocalDateTime.now() : VIRTUAL_TIME_PROVIDER.now();
    }

    // ---- 虚拟时间 ----
    private static class VirtualTimeProvider {
        private static final String STORE_FOLDER = "./simulation";
        private static final String STORE_FILE = "virtual_time.txt";

        private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        private static final Path STORE = Paths.get(STORE_FOLDER, STORE_FILE);

        private final Object lock = new Object();
        private LocalDateTime current;

        VirtualTimeProvider() {
            this.current = loadFromFile().orElse(LocalDateTime.now());
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "virtual-time-scheduler");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(() -> {
                synchronized (lock) {
                    current = current.plusDays(1);
                    try {
                        persistToFile();
                    } catch (IOException e) {
                        System.err.println("Failed to persist virtual time: " + e.getMessage());
                    }
                }
            }, 5, 5, TimeUnit.MINUTES);
        }

        public LocalDateTime now() {
            synchronized (lock) {
                return current;
            }
        }

        private void persistToFile() throws IOException {
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
