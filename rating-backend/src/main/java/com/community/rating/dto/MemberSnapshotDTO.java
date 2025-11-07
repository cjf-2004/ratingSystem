package com.community.rating.dto;

import java.time.LocalDateTime;

// 模拟从外部数据源（如论坛API）拉取到的成员快照数据结构
// memberId 现为 Long 类型，名称字段为 name
public record MemberSnapshotDTO(
    Long memberId,
    String name,
    LocalDateTime snapshotTime
) { }