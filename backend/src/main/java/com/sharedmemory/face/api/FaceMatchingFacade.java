package com.sharedmemory.face.api;

import java.util.UUID;

/**
 * The only contract the rest of the system may use to obtain face-matching results. Implementations
 * must produce participant matches only — never a sharing/distribution decision (see README.md).
 */
public interface FaceMatchingFacade {
    PhotoMatchResult detectAndMatch(UUID photoId);
}
