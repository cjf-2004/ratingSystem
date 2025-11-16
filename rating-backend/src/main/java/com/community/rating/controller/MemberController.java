package com.community.rating.controller;

import com.community.rating.dto.MemberDTO;
import com.community.rating.dto.CommonResponse;
import com.community.rating.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{member_id}")
    public ResponseEntity<CommonResponse<MemberDTO>> getMember(@PathVariable("member_id") Long memberId) {
        MemberDTO dto = memberService.getMember(memberId);
        // 根据需求：id不存在时返回空值（这里返回 200 + null body）
        return ResponseEntity.ok(CommonResponse.success(dto));
    }

    @GetMapping("/search")
    public ResponseEntity<CommonResponse<List<MemberDTO>>> searchMembers(@RequestParam String keyword,
                                                                         @RequestParam(required = false) String domain,
                                                                         @RequestParam(required = false) Integer count) {
        List<MemberDTO> list = memberService.searchMembers(keyword, domain, count);
        return ResponseEntity.ok(CommonResponse.success(list));
    }
}
