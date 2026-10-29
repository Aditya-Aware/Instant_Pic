package com.sharedmemory.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateEventRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String description,
        LocalDate startDate,
        LocalDate endDate
) {
}
