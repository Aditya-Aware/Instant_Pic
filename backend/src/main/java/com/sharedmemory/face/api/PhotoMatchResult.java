package com.sharedmemory.face.api;

import java.util.List;
import java.util.UUID;

public record PhotoMatchResult(
        UUID photoId,
        UUID eventId,
        UUID uploaderMemberId,
        List<PhotoFaceMatch> matches,
        int unmatchedFaceCount
) {
}
