package com.community.rating.repository;

import com.community.rating.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    // Member (单表): countTotalMembers()
    default Long countTotalMembers() {
        return count();
    }
}