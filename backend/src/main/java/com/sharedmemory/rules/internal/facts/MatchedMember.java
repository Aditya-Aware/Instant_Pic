package com.sharedmemory.rules.internal.facts;

import java.util.UUID;

public class MatchedMember {
    private final UUID memberId;
    private final double confidence;
    private final boolean optedOut;

    public MatchedMember(UUID memberId, double confidence, boolean optedOut) {
        this.memberId = memberId;
        this.confidence = confidence;
        this.optedOut = optedOut;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public double getConfidence() {
        return confidence;
    }

    public boolean isOptedOut() {
        return optedOut;
    }
}
