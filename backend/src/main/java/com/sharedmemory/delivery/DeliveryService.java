package com.sharedmemory.delivery;

import com.sharedmemory.media.Photo;
import com.sharedmemory.media.PhotoService;
import com.sharedmemory.rules.api.SharingDecision;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final PhotoService photoService;

    public DeliveryService(DeliveryRepository deliveryRepository, PhotoService photoService) {
        this.deliveryRepository = deliveryRepository;
        this.photoService = photoService;
    }

    @Transactional
    public List<Delivery> applyDecisions(List<SharingDecision> decisions) {
        Instant now = Instant.now();
        return decisions.stream().map(decision -> {
            Delivery delivery = deliveryRepository
                    .findByPhotoIdAndRecipientMemberId(decision.photoId(), decision.recipientMemberId())
                    .orElseGet(() -> Delivery.builder()
                            .photoId(decision.photoId())
                            .recipientMemberId(decision.recipientMemberId())
                            .build());
            delivery.setReason(decision.reason());
            delivery.setRuleName(decision.ruleName());
            delivery.setDecidedAt(now);
            if (decision.requiresApproval()) {
                delivery.setStatus(DeliveryStatus.AWAITING_APPROVAL);
            } else {
                delivery.setStatus(DeliveryStatus.DELIVERED);
                delivery.setDeliveredAt(now);
            }
            return deliveryRepository.save(delivery);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Photo> getMyMoments(UUID recipientMemberId, Pageable pageable) {
        return deliveryRepository
                .findByRecipientMemberIdAndStatusOrderByDecidedAtDesc(
                        recipientMemberId, DeliveryStatus.DELIVERED, pageable)
                .stream()
                .map(delivery -> photoService.getById(delivery.getPhotoId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Delivery> getPendingDeliveries(UUID recipientMemberId, Pageable pageable) {
        return deliveryRepository.findByRecipientMemberIdAndStatusOrderByDecidedAtDesc(
                recipientMemberId, DeliveryStatus.PENDING, pageable);
    }
}
