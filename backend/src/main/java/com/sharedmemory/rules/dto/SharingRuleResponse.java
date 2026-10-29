package com.sharedmemory.rules.dto;

import com.sharedmemory.rules.SharingRule;
import java.util.UUID;

public record SharingRuleResponse(UUID id, String ruleType, boolean enabled, int priority, String configJson) {
    public static SharingRuleResponse from(SharingRule rule) {
        return new SharingRuleResponse(
                rule.getId(), rule.getRuleType().name(), rule.isEnabled(), rule.getPriority(), rule.getConfigJson());
    }
}
