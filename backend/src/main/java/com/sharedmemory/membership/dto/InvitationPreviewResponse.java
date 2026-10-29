package com.sharedmemory.membership.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationPreviewResponse(
        UUID eventId,
        String eventName,
        String organizerDisplayName,
        long memberCount,
        Instant expiresAt
) {
}
