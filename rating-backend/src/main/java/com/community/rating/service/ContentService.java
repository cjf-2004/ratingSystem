// com.community.rating.service.ContentService.java
package com.community.rating.service;

import com.community.rating.dto.ContentDTO;
import java.util.List;

public interface ContentService {
    
    /**
     * 获取内容排名列表，按分数 (cis_score) 排序
     */
    List<ContentDTO> getContentRanking(Integer count, String sort_order);

    /**
     * 根据内容 ID 获取单个内容详情
     */
    ContentDTO getContentById(Long contentId);
}