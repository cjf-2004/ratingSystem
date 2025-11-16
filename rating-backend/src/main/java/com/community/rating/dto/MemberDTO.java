package com.community.rating.dto;

import lombok.Data;

/**
 * MemberDTO: 按用户要求的字段封装
 */
@Data
public class MemberDTO {
    private Long member_id;
    private String member_name;
    private Integer rank;
    private String main_domain;
    private String level;
    private Integer score;
    private String join_time;
}
