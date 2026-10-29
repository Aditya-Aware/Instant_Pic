package com.sharedmemory.membership.dto;

import com.sharedmemory.membership.EventMember;
import java.time.Instant;
import java.util.UUID;

public record EventMemberResponse(
        UUID id,
        UUID eventId,
        UUID userId,
        String displayName,
        String role,
        String status,
        Instant joinedAt
) {
    public static EventMemberResponse from(EventMember member) {
        return new EventMemberResponse(
                member.getId(),
                member.getEventId(),
                member.getUserId(),
                member.getDisplayName(),
                member.getRole().name(),
                member.getStatus().name(),
                member.getJoinedAt());
    }
}
