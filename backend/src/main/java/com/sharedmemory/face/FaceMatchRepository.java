package com.sharedmemory.face;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaceMatchRepository extends JpaRepository<FaceMatch, UUID> {
}
