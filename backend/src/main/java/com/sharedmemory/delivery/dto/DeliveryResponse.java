package com.sharedmemory.delivery.dto;

import com.sharedmemory.delivery.Delivery;
import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID photoId,
        UUID recipientMemberId,
        String status,
        String reason,
        String ruleName,
        Instant decidedAt,
        Instant deliveredAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getPhotoId(),
                delivery.getRecipientMemberId(),
                delivery.getStatus().name(),
                delivery.getReason(),
                delivery.getRuleName(),
                delivery.getDecidedAt(),
                delivery.getDeliveredAt());
    }
}
