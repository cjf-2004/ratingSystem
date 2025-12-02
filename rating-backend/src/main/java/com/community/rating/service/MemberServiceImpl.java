package com.community.rating.service;

import com.community.rating.dto.MemberDTO;
import com.community.rating.dto.MemberScoreHistoryDTO;
import com.community.rating.dto.ScoreHistoryItemDTO;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.BigDecimal;

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
            // 根据领域名称查到 areaId
            Optional<KnowledgeArea> areaOpt = knowledgeAreaRepository.findByAreaName(domain);
            if (areaOpt.isPresent()) {
                Integer areaId = areaOpt.get().getAreaId();
                // 获取该领域所有成员的所有评级记录
                List<MemberRating> allRatings = memberRatingRepository.findByAreaIdOrderByDesScoreDesc(areaId);
                
                // 按成员ID分组，只保留每个成员在该领域的最新评级
                Map<Long, MemberRating> latestRatingsByMember = allRatings.stream()
                    .collect(Collectors.toMap(
                        MemberRating::getMemberId,
                        rating -> rating,
                        (existing, replacement) -> {
                            // 保留更新日期较新的记录
                            if (existing.getUpdateDate() != null && replacement.getUpdateDate() != null) {
                                return existing.getUpdateDate().isAfter(replacement.getUpdateDate()) ? existing : replacement;
                            }
                            return existing; // 处理日期为空的情况
                        }
                    ));
                
                // 将最新评级转换为列表
                List<MemberRating> ratings = new ArrayList<>(latestRatingsByMember.values());
                
                // 根据排序参数进行排序
                if ("asc".equalsIgnoreCase(sort_by)) {
                    ratings.sort(Comparator.comparing(MemberRating::getDesScore, Comparator.nullsLast(BigDecimal::compareTo)));
                } else {
                    ratings.sort(Comparator.comparing(MemberRating::getDesScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
                }
                
                // 构建结果列表
                int idx = 1;
                for (MemberRating r : ratings) {
                    if (result.size() >= limit) break;
                    MemberDTO dto = buildDTOFromRating(r, idx);
                    result.add(dto);
                    idx++;
                }
            }
            return result;
        }
        // 找不到领域名称 => 返回空列表
        return result;
    }

    @Override
    public List<MemberScoreHistoryDTO> getMember(Long member_id) {
        if (member_id == null) return new ArrayList<>();
        Optional<Member> mOpt = memberRepository.findById(member_id);
        if (mOpt.isEmpty()) return new ArrayList<>();

        Member member = mOpt.get();
        List<MemberScoreHistoryDTO> resultList = new ArrayList<>();

        // 获取成员在所有领域的所有评级记录
        List<MemberRating> allRatings = memberRatingRepository.findAllByMemberId(member_id);
        
        // 按领域分组
        Map<Integer, List<MemberRating>> ratingsByArea = allRatings.stream()
                .filter(r -> r.getAreaId() != null)
                .collect(Collectors.groupingBy(MemberRating::getAreaId));

        // 为每个领域创建DTO
        for (Map.Entry<Integer, List<MemberRating>> entry : ratingsByArea.entrySet()) {
            Integer areaId = entry.getKey();
            List<MemberRating> areaRatings = entry.getValue();
            
            // 获取领域名称
            String areaName = knowledgeAreaRepository.findById(areaId)
                    .map(KnowledgeArea::getAreaName)
                    .orElse("未知领域");
            
            // 按更新日期降序排序，获取最新评级作为当前评级
            areaRatings.sort(Comparator.comparing(MemberRating::getUpdateDate).reversed());
            MemberRating latestRating = areaRatings.get(0);
            
            // 创建DTO并填充基础信息
            MemberScoreHistoryDTO dto = new MemberScoreHistoryDTO();
            dto.setMember_id(member.getMemberId());
            dto.setMember_name(member.getName());
            dto.setJoin_time(member.getJoinDate() != null ? 
                    member.getJoinDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            
            // 填充领域特定信息
            dto.setMain_domain(areaName);
            dto.setLevel(latestRating.getRatingLevel());
            dto.setScore(latestRating.getDesScore() != null ? 
                    latestRating.getDesScore().intValue() : null);
            
            // 计算该成员在当前领域的排名
            int rank = calculateDomainRank(areaId, latestRating.getDesScore());
            dto.setRank(rank);
            
            // 创建历史分数记录列表
            List<ScoreHistoryItemDTO> historyList = areaRatings.stream()
                    .map(rating -> new ScoreHistoryItemDTO(
                            rating.getUpdateDate() != null ? 
                                    rating.getUpdateDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null,
                            rating.getDesScore() != null ? 
                                    rating.getDesScore().doubleValue() : null
                    ))
                    .collect(Collectors.toList());
            
            dto.setScore_history(historyList);
            resultList.add(dto);
        }

        // 如果该成员没有任何评级记录，创建一个基本的DTO
        if (resultList.isEmpty()) {
            MemberScoreHistoryDTO basicDto = new MemberScoreHistoryDTO();
            basicDto.setMember_id(member.getMemberId());
            basicDto.setMember_name(member.getName());
            basicDto.setJoin_time(member.getJoinDate() != null ? 
                    member.getJoinDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            basicDto.setScore_history(new ArrayList<>());
            resultList.add(basicDto);
        }

        return resultList;
    }
    
    /**
     * 计算成员在特定领域的排名
     * @param areaId 领域ID
     * @param memberScore 当前成员的分数
     * @return 排名（从1开始）
     */
    private int calculateDomainRank(Integer areaId, BigDecimal memberScore) {
        if (areaId == null || memberScore == null) {
            return 0; // 无效数据返回0
        }
        
        // 获取该领域所有成员的所有评级记录
        List<MemberRating> allAreaRatings = memberRatingRepository.findByAreaIdOrderByDesScoreDesc(areaId);
        
        // 按成员ID分组，只保留每个成员在该领域的最新评级
        Map<Long, MemberRating> latestRatingsByMember = allAreaRatings.stream()
            .collect(Collectors.toMap(
                MemberRating::getMemberId,
                rating -> rating,
                (existing, replacement) -> {
                    // 保留更新日期较新的记录
                    if (existing.getUpdateDate() != null && replacement.getUpdateDate() != null) {
                        return existing.getUpdateDate().isAfter(replacement.getUpdateDate()) ? existing : replacement;
                    }
                    return existing; // 处理日期为空的情况
                }
            ));
        
        // 将最新评级转换为列表并按分数降序排序
        List<MemberRating> latestRatings = latestRatingsByMember.values().stream()
            .filter(rating -> rating.getDesScore() != null)
            .sorted((r1, r2) -> r2.getDesScore().compareTo(r1.getDesScore()))
            .collect(Collectors.toList());
        
        // 计算排名
        int rank = 1;
        for (MemberRating rating : latestRatings) {
            if (rating.getDesScore().compareTo(memberScore) > 0) {
                rank++;
            } else {
                break;
            }
        }
        return rank;
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