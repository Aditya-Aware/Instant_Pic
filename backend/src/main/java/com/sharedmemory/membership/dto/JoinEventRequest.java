package com.sharedmemory.membership.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinEventRequest(
        @NotBlank String code,
        @NotBlank @Size(max = 120) String displayName
) {
}
