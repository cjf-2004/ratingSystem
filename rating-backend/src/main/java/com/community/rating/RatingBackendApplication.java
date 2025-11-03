package com.community.rating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 导入

@SpringBootApplication
@EnableScheduling // 【新增】启用 Spring 定时任务功能
public class RatingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RatingBackendApplication.class, args);
        System.out.println("Rating Backend Application is running on port 8080...");
    }

}