package com.sharedmemory.membership;

import com.sharedmemory.identity.security.CurrentUserId;
import com.sharedmemory.membership.dto.EventMemberResponse;
import com.sharedmemory.membership.dto.JoinEventRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping("/api/events/join")
    public ResponseEntity<EventMemberResponse> join(@Valid @RequestBody JoinEventRequest request) {
        EventMember member = membershipService.joinByInvitationCode(
                CurrentUserId.get(), request.code(), request.displayName());
        return ResponseEntity.ok(EventMemberResponse.from(member));
    }

    @GetMapping("/api/events/{eventId}/members")
    public ResponseEntity<List<EventMemberResponse>> listMembers(@PathVariable UUID eventId) {
        List<EventMemberResponse> members = membershipService.listActiveMembers(eventId, CurrentUserId.get())
                .stream()
                .map(EventMemberResponse::from)
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/api/events/{eventId}/leave")
    public ResponseEntity<Void> leave(@PathVariable UUID eventId) {
        membershipService.leaveEvent(eventId, CurrentUserId.get());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/events/{eventId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID eventId, @PathVariable UUID memberId) {
        membershipService.removeMember(eventId, CurrentUserId.get(), memberId);
        return ResponseEntity.noContent().build();
    }
}
