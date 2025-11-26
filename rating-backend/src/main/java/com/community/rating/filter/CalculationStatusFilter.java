package com.community.rating.filter;

import com.community.rating.dto.CommonResponse;
import com.community.rating.util.CalculationStatusManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 计算状态过滤器 - 拦截所有API请求，检查评分计算状态
 * 当计算进行中时，直接返回423状态码（资源被锁定）
 */
@Component
@Order(1) // 确保在其他过滤器之前执行
public class CalculationStatusFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(CalculationStatusFilter.class);
    
    @Autowired
    private CalculationStatusManager statusManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 检查评分计算是否正在进行
        if (statusManager.isCalculationInProgress()) {
            // 记录拦截的请求
            log.warn("拦截请求: {} {} (评分计算正在进行)", 
                    httpRequest.getMethod(), httpRequest.getRequestURI());
            
            // 设置响应状态码为423 (Locked)
            httpResponse.setStatus(423);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
            
            // 创建错误响应体
            CommonResponse<String> errorResponse = CommonResponse.error(
                    423, "系统正在进行评分计算，请稍后再试");
            
            // 写入响应体
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            httpResponse.getWriter().write(jsonResponse);
            
            return; // 结束过滤器链，不继续处理请求
        }
        
        // 如果计算未进行，则继续处理请求
        chain.doFilter(request, response);
    }
}