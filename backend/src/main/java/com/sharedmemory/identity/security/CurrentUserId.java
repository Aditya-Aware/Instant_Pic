package com.sharedmemory.identity.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserId {

    private CurrentUserId() {
    }

    public static UUID get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UUID userId) {
            return userId;
        }
        throw new IllegalStateException("No authenticated user in context");
    }
}
