package com.sharedmemory.media;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    List<Photo> findByEventIdAndGalleryVisibleTrueOrderByCreatedAtDesc(UUID eventId, Pageable pageable);
}
