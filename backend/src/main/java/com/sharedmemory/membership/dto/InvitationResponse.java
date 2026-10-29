package com.sharedmemory.membership.dto;

import com.sharedmemory.membership.Invitation;
import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID eventId,
        String code,
        Instant expiresAt,
        Integer maxUses,
        int usesCount,
        String status
) {
    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getEventId(),
                invitation.getCode(),
                invitation.getExpiresAt(),
                invitation.getMaxUses(),
                invitation.getUsesCount(),
                invitation.getStatus().name());
    }
}
