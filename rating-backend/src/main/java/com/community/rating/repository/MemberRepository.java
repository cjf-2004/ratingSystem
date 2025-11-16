package com.community.rating.repository;

import com.community.rating.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    // Member (单表): countTotalMembers()
    default Long countTotalMembers() {
        return count();
    }
    // Spring Data JPA 会自动提供 findById, save, existsById 等方法
    // ID 类型已更改为 Long
    Optional<Member> findById(Long memberId);
    
    // 根据名字模糊搜索（不区分大小写）
    java.util.List<Member> findByNameContainingIgnoreCase(String namePart);
}