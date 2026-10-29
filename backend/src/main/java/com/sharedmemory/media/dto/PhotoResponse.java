package com.sharedmemory.media.dto;

import com.sharedmemory.media.Photo;
import java.time.Instant;
import java.util.UUID;

public record PhotoResponse(
        UUID id,
        UUID eventId,
        UUID uploadedByMemberId,
        Integer width,
        Integer height,
        String processingStatus,
        boolean galleryVisible,
        Instant createdAt
) {
    public static PhotoResponse from(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getEventId(),
                photo.getUploadedByMemberId(),
                photo.getWidth(),
                photo.getHeight(),
                photo.getProcessingStatus().name(),
                photo.isGalleryVisible(),
                photo.getCreatedAt());
    }
}
