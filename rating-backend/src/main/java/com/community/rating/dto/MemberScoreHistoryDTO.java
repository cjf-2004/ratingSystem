package com.community.rating.dto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 成员详情 DTO，包含分领域的当前评级和历史分数记录。
 * 用于 getMember 接口，一个实例代表成员在一个特定领域的数据。
 */
@Data
@EqualsAndHashCode(callSuper = true) // 继承 MemberDTO 的字段
@NoArgsConstructor
@AllArgsConstructor
public class MemberScoreHistoryDTO extends MemberDTO {
    
    // 成员 DES 历史分数记录列表。
    // 列表中的所有记录都属于由本实例的 main_domain 字段所代表的领域。
    private List<ScoreHistoryItemDTO> score_history; 
    
    // 注意：所有父类字段 (member_id, member_name, rank, main_domain, level, score, join_time) 
    // 都会被继承，且用于承载该特定领域的信息。
}