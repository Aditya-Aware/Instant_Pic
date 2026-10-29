package com.sharedmemory.face;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaceProfileRepository extends JpaRepository<FaceProfile, UUID> {
    Optional<FaceProfile> findByEventMemberId(UUID eventMemberId);

    List<FaceProfile> findByEventMemberIdInAndStatus(List<UUID> eventMemberIds, FaceProfileStatus status);
}
