// File: ContentSnapshotRepository.java
package com.community.rating.repository;

import com.community.rating.entity.ContentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository; // 导入 Repository
import java.time.LocalDateTime;

import java.util.List;

@Repository // 添加 @Repository
public interface ContentSnapshotRepository extends JpaRepository<ContentSnapshot, Long> {

    // ContentSnapshot (单表): countByPublishTimeAfter()
    Long countByPublishTimeAfter(LocalDateTime time);

    // ContentSnapshot (单表/跨字段统计): countDistinctMemberIdByPublishTimeAfter()
    @Query(value = "SELECT COUNT(DISTINCT c.member_id) FROM contentsnapshot c WHERE c.publish_time > :time", nativeQuery = true)
    Long countDistinctMemberIdByPublishTimeAfter(LocalDateTime time);

    /**
     * 用于 DES 计算：获取所有已计算 CIS 结果的内容。
     * @return 包含 CIS 分数的所有内容快照实体
     */
    List<ContentSnapshot> findAllByCisScoreIsNotNull();

    // 统计某成员发布的内容数量
    Long countByMemberId(Long memberId);

    // 按成员与帖子长度分级统计数量
    Long countByMemberIdAndPostLengthLevel(Long memberId, Integer postLengthLevel);

    // 按成员获取内容，按发布时间倒序
    List<ContentSnapshot> findByMemberIdOrderByPublishTimeDesc(Long memberId);

    // 按成员与时间区间查询
    List<ContentSnapshot> findByMemberIdAndPublishTimeBetween(Long memberId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    // ----------------- Database-level aggregations (native queries) -----------------

    // Return member_ids whose total post count >= :threshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT c.member_id FROM contentsnapshot c GROUP BY c.member_id HAVING COUNT(*) >= :threshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsByPostCountGreaterThanEqual(long threshold);

    // Return member_ids whose cumulative likes >= :threshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT c.member_id FROM contentsnapshot c GROUP BY c.member_id HAVING SUM(COALESCE(c.like_count_snapshot,0)) >= :threshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsByCumulativeLikesGreaterThanEqual(long threshold);

    // Return member_ids whose cumulative comments >= :threshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT c.member_id FROM contentsnapshot c GROUP BY c.member_id HAVING SUM(COALESCE(c.comment_count_snapshot,0)) >= :threshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsByCumulativeCommentsGreaterThanEqual(long threshold);

    // Return member_ids whose cumulative shares >= :threshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT c.member_id FROM contentsnapshot c GROUP BY c.member_id HAVING SUM(COALESCE(c.share_count_snapshot,0)) >= :threshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsByCumulativeSharesGreaterThanEqual(long threshold);

    // Return member_ids who have any single post with likes >= :likeThreshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT c.member_id FROM contentsnapshot c WHERE COALESCE(c.like_count_snapshot,0) >= :likeThreshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsWithAnyPostLikeAtLeast(int likeThreshold);

    // Return member_ids who have any single post with total interactions (likes+comments+shares) >= :threshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT c.member_id FROM contentsnapshot c WHERE (COALESCE(c.like_count_snapshot,0) + COALESCE(c.comment_count_snapshot,0) + COALESCE(c.share_count_snapshot,0)) >= :threshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsWithAnyPostEngagementAtLeast(int threshold);

    // Return member_ids who have any single post with comment_count >= :commentThreshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT c.member_id FROM contentsnapshot c WHERE COALESCE(c.comment_count_snapshot,0) >= :commentThreshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsWithAnyPostCommentAtLeast(int commentThreshold);

    // Return member_ids who have any single post with share_count >= :shareThreshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT c.member_id FROM contentsnapshot c WHERE COALESCE(c.share_count_snapshot,0) >= :shareThreshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsWithAnyPostShareAtLeast(int shareThreshold);

    // Return distinct member_ids who had posts since :since and like_count >= :likeThreshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT c.member_id FROM contentsnapshot c WHERE c.publish_time > :since AND COALESCE(c.like_count_snapshot,0) >= :likeThreshold", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsWithRecentPostLikeAtLeast(java.time.LocalDateTime since, int likeThreshold);

    // Return member_ids who have any single day where post count >= :perDayThreshold
    @org.springframework.data.jpa.repository.Query(value = "SELECT member_id FROM (SELECT c.member_id, DATE(c.publish_time) as d, COUNT(*) as cnt FROM contentsnapshot c GROUP BY c.member_id, DATE(c.publish_time)) t WHERE t.cnt >= :perDayThreshold GROUP BY t.member_id", nativeQuery = true)
    java.util.List<java.lang.Long> findMemberIdsWithAnyDayPostCountAtLeast(int perDayThreshold);
}