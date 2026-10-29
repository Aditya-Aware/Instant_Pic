package com.sharedmemory.rules.api;

import java.util.UUID;

public record VisibilityDecision(UUID photoId, boolean galleryVisible, String ruleName) {
}
