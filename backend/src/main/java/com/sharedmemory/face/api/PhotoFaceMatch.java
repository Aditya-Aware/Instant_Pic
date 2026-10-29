package com.sharedmemory.face.api;

import java.util.UUID;

public record PhotoFaceMatch(UUID photoFaceId, UUID eventMemberId, double confidence) {
}
