package com.sharedmemory.membership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventMemberRepository extends JpaRepository<EventMember, UUID> {
    Optional<EventMember> findByEventIdAndUserId(UUID eventId, UUID userId);

    List<EventMember> findByEventIdAndStatus(UUID eventId, MemberStatus status);

    List<EventMember> findByUserIdAndStatus(UUID userId, MemberStatus status);

    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, MemberStatus status);
}
