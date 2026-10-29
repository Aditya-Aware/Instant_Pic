package com.sharedmemory.face;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoFaceRepository extends JpaRepository<PhotoFace, UUID> {
    List<PhotoFace> findByPhotoId(UUID photoId);
}
