package com.sharedmemory.rules.internal.facts;

import java.util.UUID;

public class SharingRuleConfig {
    private final UUID eventId;
    private final String ruleType;
    private final boolean enabled;

    public SharingRuleConfig(UUID eventId, String ruleType, boolean enabled) {
        this.eventId = eventId;
        this.ruleType = ruleType;
        this.enabled = enabled;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getRuleType() {
        return ruleType;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
