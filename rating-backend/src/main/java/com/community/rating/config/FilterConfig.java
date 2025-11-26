package com.community.rating.config;

import com.community.rating.filter.CalculationStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    
    @Autowired
    private CalculationStatusFilter calculationStatusFilter;
    
    @Bean
    public FilterRegistrationBean<CalculationStatusFilter> calculationStatusFilterBean() {
        FilterRegistrationBean<CalculationStatusFilter> registrationBean = 
                new FilterRegistrationBean<>(calculationStatusFilter);
        
        // 配置拦截路径 - 可以只拦截API请求
        registrationBean.addUrlPatterns("/api/*");
        
        // 设置优先级
        registrationBean.setOrder(1);
        
        return registrationBean;
    }
}