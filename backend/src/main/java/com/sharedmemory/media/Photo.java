package com.sharedmemory.media;

import com.sharedmemory.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Photo extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "uploaded_by_member_id", nullable = false)
    private UUID uploadedByMemberId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    @Column(name = "captured_at")
    private Instant capturedAt;

    private Integer width;

    private Integer height;

    @Column(nullable = false)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    @Builder.Default
    private PhotoProcessingStatus processingStatus = PhotoProcessingStatus.PENDING;

    @Column(name = "gallery_visible", nullable = false)
    @Builder.Default
    private boolean galleryVisible = false;
}
