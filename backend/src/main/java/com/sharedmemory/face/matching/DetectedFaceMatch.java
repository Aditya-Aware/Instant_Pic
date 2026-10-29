package com.sharedmemory.face.matching;

import com.sharedmemory.face.detection.FaceBox;
import java.util.Optional;
import java.util.UUID;

public record DetectedFaceMatch(FaceBox box, float[] embedding, Optional<UUID> matchedProfileId, double bestScore) {
}
