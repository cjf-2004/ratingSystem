// com.community.rating.service.AchievementServiceImpl.java
package com.community.rating.service;

import com.community.rating.dto.AchievementDTO;
import com.community.rating.repository.AchievementStatus_AchievementDefinition_Repository;
import com.community.rating.repository.MemberRepository; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AchievementServiceImpl implements AchievementService {

    private final AchievementStatus_AchievementDefinition_Repository customRepository;
    private final MemberRepository memberRepository;

    public AchievementServiceImpl(AchievementStatus_AchievementDefinition_Repository customRepository, 
                                  MemberRepository memberRepository) {
        this.customRepository = customRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 辅助方法：将 Object[] 结果集转换为 AchievementDTO
     * 索引顺序: [0:achievement_key, 1:name, 2:type, 3:trigger_condition_desc, 4:achieved_count]
     * @param result Object[] 数组
     * @param finalTotalMembers 总成员数 (用于计算完成率)
     * @return AchievementDTO 实例
     */
    private AchievementDTO mapToObjectArrayToDTO(Object[] result, double finalTotalMembers) {
        AchievementDTO dto = new AchievementDTO();
        
        // 索引 0: achievement_key (String)
        dto.setAchievementKey((String) result[0]);
        
        // 索引 1: name (String)
        dto.setName((String) result[1]);
        
        // 索引 2: type (String) -> 映射为 category
        dto.setCategory((String) result[2]); 
        
        // 索引 3: trigger_condition_desc (String) -> 映射为 description
        dto.setDescription((String) result[3]);

        // 索引 4: achieved_count (来自 COUNT，通常为 Long 或 BigInteger，转换为 Integer)
        // 使用 Number 保证兼容性
        Integer achievedCount = ((Number) result[4]).intValue();
        dto.setAchievedCount(achievedCount);
        
        // 计算完成率
        Double completionRate = achievedCount / finalTotalMembers;
        dto.setCompletionRate(completionRate);
        dto.setRank(null);
        
        return dto;
    }
    
    /**
     * 辅助方法：计算所有成就的DTO，用于 List 和 Ranking 的基准数据
     */
    private List<AchievementDTO> buildAllAchievementDTOs() {
        // 1. 获取总成员数
        long totalMembers = memberRepository.count();
        final double finalTotalMembers = (totalMembers == 0) ? 1.0 : (double) totalMembers;

        // 2. 调用联查方法获取所有定义和统计数据
        List<Object[]> results = customRepository.findAllAchievementsWithStats();

        // 3. 转换 DTO
        return results.stream()
            .map(result -> mapToObjectArrayToDTO(result, finalTotalMembers))
            .collect(Collectors.toList());
    }


    @Override
    public List<AchievementDTO> getAchievementList() {
        // 直接返回所有成就的DTO (无排名)
        return buildAllAchievementDTOs();
    }

    @Override
    public List<AchievementDTO> getAchievementRanking(Integer count, String sortOrder) {
        // 使用 buildAllAchievementDTOs 获取所有数据
        List<AchievementDTO> allAchievements = buildAllAchievementDTOs();

        // 1. 参数默认值和校验
        final int finalCount = (count == null || count < 1) ? 1 : count;
        
        // 2. 根据 sortOrder 确定排序方式: 按 AchievedCount 排序
        Comparator<AchievementDTO> comparator = Comparator.comparing(AchievementDTO::getAchievedCount);
        
        // sortOrder 默认 "desc" (从多到少)
        if (sortOrder == null || sortOrder.equalsIgnoreCase("desc")) {
            comparator = comparator.reversed();
        } 
        
        // 3. 排序
        List<AchievementDTO> rankedList = allAchievements.stream()
                                            .sorted(comparator)
                                            .collect(Collectors.toList());
        
        // 4. 截断列表
        List<AchievementDTO> subList = rankedList.size() > finalCount ? 
                                       rankedList.subList(0, finalCount) : 
                                       rankedList;

        // 5. 设置排名
        for (int i = 0; i < subList.size(); i++) {
            subList.get(i).setRank(i + 1);
        }

        return subList;
    }
}