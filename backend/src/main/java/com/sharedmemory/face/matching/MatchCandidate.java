package com.sharedmemory.face.matching;

import java.util.UUID;

public record MatchCandidate(UUID profileId, float[] embedding) {
}
