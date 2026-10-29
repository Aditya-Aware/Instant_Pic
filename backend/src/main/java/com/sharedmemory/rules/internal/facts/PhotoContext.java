package com.sharedmemory.rules.internal.facts;

import java.util.List;
import java.util.UUID;

public class PhotoContext {
    private final UUID photoId;
    private final UUID eventId;
    private final int matchedMemberCount;
    private final List<MatchedMember> matchedMembers;

    public PhotoContext(UUID photoId, UUID eventId, List<MatchedMember> matchedMembers) {
        this.photoId = photoId;
        this.eventId = eventId;
        this.matchedMembers = matchedMembers;
        this.matchedMemberCount = matchedMembers.size();
    }

    public UUID getPhotoId() {
        return photoId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public int getMatchedMemberCount() {
        return matchedMemberCount;
    }

    public List<MatchedMember> getMatchedMembers() {
        return matchedMembers;
    }
}
