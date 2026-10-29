package com.sharedmemory.face.dto;

import com.sharedmemory.face.FaceProfile;
import java.time.Instant;

public record FaceEnrollmentResponse(String status, int sampleCount, Instant updatedAt) {
    public static FaceEnrollmentResponse from(FaceProfile profile) {
        return new FaceEnrollmentResponse(
                profile.getStatus().name(), profile.getSampleCount(), profile.getUpdatedAt());
    }

    public static FaceEnrollmentResponse notEnrolled() {
        return new FaceEnrollmentResponse("NOT_ENROLLED", 0, null);
    }
}
