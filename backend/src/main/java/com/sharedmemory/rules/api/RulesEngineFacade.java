package com.sharedmemory.rules.api;

import com.sharedmemory.face.api.PhotoMatchResult;
import java.util.List;

/**
 * The only contract the rest of the system may use to turn face matches into a sharing outcome.
 * Consumes participant matches (never face-matching internals) and decides who receives the
 * photo and whether it appears in the shared event gallery (see README.md).
 */
public interface RulesEngineFacade {
    List<SharingDecision> evaluate(PhotoMatchResult matchResult);
}
