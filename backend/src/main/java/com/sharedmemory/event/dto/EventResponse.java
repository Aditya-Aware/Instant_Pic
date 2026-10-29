package com.sharedmemory.event.dto;

import com.sharedmemory.event.Event;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        String description,
        UUID createdByUserId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Instant createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCreatedByUserId(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStatus().name(),
                event.getCreatedAt());
    }
}
