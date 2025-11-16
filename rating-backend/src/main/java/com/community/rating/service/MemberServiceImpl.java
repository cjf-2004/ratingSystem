package com.community.rating.service;

import com.community.rating.dto.MemberDTO;
import com.community.rating.entity.KnowledgeArea;
import com.community.rating.entity.Member;
import com.community.rating.entity.MemberRating;
import com.community.rating.repository.KnowledgeAreaRepository;
import com.community.rating.repository.MemberRatingRepository;
import com.community.rating.repository.MemberRepository;
import com.community.rating.repository.Member_MemberRating_KnowledgeArea_Repository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberRatingRepository memberRatingRepository;
    private final KnowledgeAreaRepository knowledgeAreaRepository;
    private final Member_MemberRating_KnowledgeArea_Repository combinedRepo;

    public MemberServiceImpl(MemberRepository memberRepository,
                             MemberRatingRepository memberRatingRepository,
                             KnowledgeAreaRepository knowledgeAreaRepository,
                             Member_MemberRating_KnowledgeArea_Repository combinedRepo) {
        this.memberRepository = memberRepository;
        this.memberRatingRepository = memberRatingRepository;
        this.knowledgeAreaRepository = knowledgeAreaRepository;
        this.combinedRepo = combinedRepo;
    }

    @Override
    public List<MemberDTO> getMemberRankingList(Integer count, String domain, String sort_by) {
        int limit = (count == null || count <= 0) ? 1 : count;

        List<MemberDTO> result = new ArrayList<>();

        if (domain != null && !domain.isEmpty()) {
            // 根据领域名称查到 areaId，然后使用 MemberRatingRepository 按 des_score 排序
            Optional<KnowledgeArea> areaOpt = knowledgeAreaRepository.findByAreaName(domain);
            if (areaOpt.isPresent()) {
                Integer areaId = areaOpt.get().getAreaId();
                List<MemberRating> ratings = memberRatingRepository.findByAreaIdOrderByDesScoreDesc(areaId);
                if ("asc".equalsIgnoreCase(sort_by)) {
                    ratings = ratings.stream()
                            .sorted(Comparator.comparing(MemberRating::getDesScore))
                            .collect(Collectors.toList());
                }
                int idx = 1;
                for (MemberRating r : ratings) {
                    if (result.size() >= limit) break;
                    MemberDTO dto = buildDTOFromRating(r, idx);
                    result.add(dto);
                    idx++;
                }
                return result;
            }
            // 找不到领域名称 => 返回空列表
            return result;
        }

        // 全局排名，使用已经存在的 native 查询（combinedRepo）
        List<Object[]> rows = combinedRepo.findTopMembersRankingData(limit);
        int idx = 1;
        for (Object[] row : rows) {
            MemberDTO dto = new MemberDTO();
            // row: m.member_id, m.name, ka.area_name, latest_rating.rating_level, latest_rating.des_score
            dto.setMember_id(((Number) row[0]).longValue());
            dto.setMember_name((String) row[1]);
            dto.setMain_domain(row[2] != null ? row[2].toString() : null);
            dto.setLevel(row[3] != null ? row[3].toString() : null);
            dto.setScore(row[4] != null ? ((Number) row[4]).intValue() : null);
            dto.setRank(idx);
            // try to get join_time from Member entity
            Optional<Member> memberOpt = memberRepository.findById(dto.getMember_id());
            memberOpt.ifPresent(m -> dto.setJoin_time(m.getJoinDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));
            result.add(dto);
            idx++;
        }

        if ("asc" .equalsIgnoreCase(sort_by)) {
            result = result.stream().sorted(Comparator.comparing(MemberDTO::getScore, Comparator.nullsLast(Integer::compareTo))).collect(Collectors.toList());
        }

        return result;
    }

    @Override
    public MemberDTO getMember(Long member_id) {
        if (member_id == null) return null;
        Optional<Member> mOpt = memberRepository.findById(member_id);
        if (mOpt.isEmpty()) return null;

        Member m = mOpt.get();
        MemberDTO dto = new MemberDTO();
        dto.setMember_id(m.getMemberId());
        dto.setMember_name(m.getName());
        dto.setJoin_time(m.getJoinDate() != null ? m.getJoinDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);

        // get latest rating for this member
        List<MemberRating> ratings = memberRatingRepository.findAllByMemberId(member_id);
        if (!ratings.isEmpty()) {
            MemberRating latest = ratings.stream().max(Comparator.comparing(MemberRating::getUpdateDate)).get();
            dto.setLevel(latest.getRatingLevel());
            dto.setScore(latest.getDesScore() != null ? latest.getDesScore().intValue() : null);
            // map areaId -> areaName
            if (latest.getAreaId() != null) {
                knowledgeAreaRepository.findById(latest.getAreaId()).ifPresent(a -> dto.setMain_domain(a.getAreaName()));
            }
        }

        return dto;
    }

    @Override
    public List<MemberDTO> searchMembers(String keyword, String domain, Integer count) {
        if (keyword == null || keyword.isBlank()) return new ArrayList<>();
        int limit = (count == null || count <= 0) ? 5 : count;

        List<com.community.rating.entity.Member> matches = memberRepository.findByNameContainingIgnoreCase(keyword);
        if (domain != null && !domain.isEmpty()) {
            Optional<KnowledgeArea> areaOpt = knowledgeAreaRepository.findByAreaName(domain);
            if (areaOpt.isPresent()) {
                Integer areaId = areaOpt.get().getAreaId();
                // filter members that have any rating in that domain
                matches = matches.stream().filter(m -> memberRatingRepository.findByMemberId(m.getMemberId()).stream().anyMatch(r -> areaId.equals(r.getAreaId()))).collect(Collectors.toList());
            } else {
                // domain specified but not found -> return empty
                return new ArrayList<>();
            }
        }

        // 计算匹配度：关键词长度占名字长度比例降序
        List<MemberDTO> dtos = matches.stream()
                .map(m -> {
                    MemberDTO dto = new MemberDTO();
                    dto.setMember_id(m.getMemberId());
                    dto.setMember_name(m.getName());
                    dto.setJoin_time(m.getJoinDate() != null ? m.getJoinDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
                    // attach latest rating if any
                    List<MemberRating> ratings = memberRatingRepository.findAllByMemberId(m.getMemberId());
                    if (!ratings.isEmpty()) {
                        MemberRating latest = ratings.stream().max(Comparator.comparing(MemberRating::getUpdateDate)).get();
                        dto.setLevel(latest.getRatingLevel());
                        dto.setScore(latest.getDesScore() != null ? latest.getDesScore().intValue() : null);
                        knowledgeAreaRepository.findById(latest.getAreaId()).ifPresent(a -> dto.setMain_domain(a.getAreaName()));
                    }
                    return dto;
                })
                .sorted((a, b) -> {
                    double scoreA = (double) (a.getMember_name().contains(keyword) ? ((double) keyword.length() / a.getMember_name().length()) : 0.0);
                    double scoreB = (double) (b.getMember_name().contains(keyword) ? ((double) keyword.length() / b.getMember_name().length()) : 0.0);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .collect(Collectors.toList());

        // set ranks sequentially
        for (int i = 0; i < dtos.size(); i++) dtos.get(i).setRank(i + 1);

        return dtos;
    }

    private MemberDTO buildDTOFromRating(MemberRating r, int rank) {
        MemberDTO dto = new MemberDTO();
        dto.setMember_id(r.getMemberId());
        dto.setLevel(r.getRatingLevel());
        dto.setScore(r.getDesScore() != null ? r.getDesScore().intValue() : null);
        dto.setRank(rank);
        if (r.getMemberId() != null) {
            memberRepository.findById(r.getMemberId()).ifPresent(m -> dto.setMember_name(m.getName()));
            memberRepository.findById(r.getMemberId()).ifPresent(m -> dto.setJoin_time(m.getJoinDate() != null ? m.getJoinDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null));
        }
        if (r.getAreaId() != null) {
            knowledgeAreaRepository.findById(r.getAreaId()).ifPresent(a -> dto.setMain_domain(a.getAreaName()));
        }
        return dto;
    }
}
