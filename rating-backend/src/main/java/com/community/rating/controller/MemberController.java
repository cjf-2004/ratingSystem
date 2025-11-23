package com.community.rating.controller;

import com.community.rating.dto.MemberDTO;
import com.community.rating.dto.MemberScoreHistoryDTO;
import com.community.rating.dto.CommonResponse;
import com.community.rating.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections; // 用于返回空列表
import java.util.List;

@RestController
@RequestMapping("/api/Member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/ranking")
    public ResponseEntity<CommonResponse<List<MemberDTO>>> getMemberRankingList(@RequestParam(required = false) Integer count,
                                                                                 @RequestParam(required = false) String domain,
                                                                                 @RequestParam(required = false, name = "sort_by") String sortBy) {
        List<MemberDTO> list = memberService.getMemberRankingList(count, domain, sortBy);
        return ResponseEntity.ok(CommonResponse.success(list));
    }

    /**
     * 获取单个成员在所有知识领域内的评级、排名和分领域历史分数记录列表。
     * 每个 MemberScoreHistoryDTO 实例代表一个领域的完整数据。
     * * @param memberId 成员唯一标识
     * @return 包含 List<MemberScoreHistoryDTO> 的 ResponseEntity
     */
    @GetMapping("/{member_id}")
    public ResponseEntity<CommonResponse<List<MemberScoreHistoryDTO>>> getMember(
            @PathVariable("member_id") Long memberId) {
        
        // 调用服务方法，现在它返回的是 List<MemberScoreHistoryDTO>
        List<MemberScoreHistoryDTO> dtoList = memberService.getMember(memberId);
        
        // 根据您的需求：“id不存在时返回空值/空列表”
        // 这里的逻辑处理为：如果 Service 返回 null 或空列表，则返回一个成功的空列表响应。
        if (dtoList == null) {
            dtoList = Collections.emptyList();
        }
        
        // 使用 CommonResponse 封装并返回 200 OK 状态
        return ResponseEntity.ok(CommonResponse.success(dtoList));
    }

    @GetMapping("/search")
    public ResponseEntity<CommonResponse<List<MemberDTO>>> searchMembers(@RequestParam String keyword,
                                                                         @RequestParam(required = false) String domain,
                                                                         @RequestParam(required = false) Integer count) {
        List<MemberDTO> list = memberService.searchMembers(keyword, domain, count);
        return ResponseEntity.ok(CommonResponse.success(list));
    }
}
