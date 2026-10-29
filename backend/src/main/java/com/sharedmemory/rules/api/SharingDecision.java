package com.sharedmemory.rules.api;

import java.util.UUID;

public record SharingDecision(
        UUID photoId,
        UUID eventId,
        UUID recipientMemberId,
        String reason,
        String ruleName,
        boolean requiresApproval
) {
}
