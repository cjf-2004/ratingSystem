package com.community.rating.service;

import com.community.rating.dto.MemberDTO;
import java.util.List;

public interface MemberService {
    List<MemberDTO> getMemberRankingList(Integer count, String domain, String sort_by);
    MemberDTO getMember(Long member_id);
    List<MemberDTO> searchMembers(String keyword, String domain, Integer count);
}
