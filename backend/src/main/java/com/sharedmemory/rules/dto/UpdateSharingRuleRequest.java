package com.sharedmemory.rules.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSharingRuleRequest(@NotNull String ruleType, @NotNull Boolean enabled) {
}
