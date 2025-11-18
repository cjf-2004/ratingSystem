package com.community.rating.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类，用于配置CORS等Web相关设置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 配置所有路径都允许跨域请求
        registry.addMapping("/**")
                // 允许的源
                .allowedOrigins("http://localhost:8000")
                // 允许的HTTP方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 是否允许携带凭证（如cookies）
                .allowCredentials(true)
                // 预检请求的有效期，单位秒
                .maxAge(3600);
    }
}