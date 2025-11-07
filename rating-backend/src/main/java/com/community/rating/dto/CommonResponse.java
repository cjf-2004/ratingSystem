// File: com/community/rating/dto/CommonResponse.java
package com.community.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 通用API响应结构 DTO
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> implements Serializable {

    // 状态码：200, 400, 401, 403, 500
    private Integer code;

    // 响应描述信息
    private String message;

    // 业务数据 (可以是任何DTO, List, 或 null)
    private T data;

    // 响应时间戳 (ms)
    private Long timestamp;

    // --- 静态构造方法 (简化成功响应) ---

    /**
     * 构造成功响应 (状态码 200)
     * @param data 业务数据
     */
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(
                200,
                "请求成功", // 对应状态码 200 的描述
                data,
                Instant.now().toEpochMilli()
        );
    }
    
    /**
     * 构造成功响应 (无数据返回)
     */
    public static CommonResponse<Void> success() {
        return success(null);
    }

    // --- 静态构造方法 (简化错误响应) ---
    
    /**
     * 构造失败响应
     * @param code 状态码 (如 400, 500)
     * @param message 错误描述
     */
    public static <T> CommonResponse<T> error(Integer code, String message) {
        return new CommonResponse<>(
                code,
                message,
                null,
                Instant.now().toEpochMilli()
        );
    }
}