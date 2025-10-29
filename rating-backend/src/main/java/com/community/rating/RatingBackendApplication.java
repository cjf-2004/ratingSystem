package com.community.rating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RatingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RatingBackendApplication.class, args);
        System.out.println("Rating Backend Application is running on port 8080...");
    }

}