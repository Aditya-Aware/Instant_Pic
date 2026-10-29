package com.sharedmemory.delivery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    Optional<Delivery> findByPhotoIdAndRecipientMemberId(UUID photoId, UUID recipientMemberId);

    List<Delivery> findByPhotoId(UUID photoId);

    List<Delivery> findByRecipientMemberIdOrderByDecidedAtDesc(UUID recipientMemberId, Pageable pageable);

    List<Delivery> findByRecipientMemberIdAndStatusOrderByDecidedAtDesc(
            UUID recipientMemberId, DeliveryStatus status, Pageable pageable);
}
