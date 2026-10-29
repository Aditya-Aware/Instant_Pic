package com.sharedmemory.membership;

import com.sharedmemory.identity.security.CurrentUserId;
import com.sharedmemory.membership.dto.InvitationPreviewResponse;
import com.sharedmemory.membership.dto.InvitationResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/api/events/{eventId}/invitations")
    public ResponseEntity<InvitationResponse> generate(@PathVariable UUID eventId) {
        Invitation invitation = invitationService.generateInvitation(eventId, CurrentUserId.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(InvitationResponse.from(invitation));
    }

    @GetMapping("/api/invitations/{code}")
    public ResponseEntity<InvitationPreviewResponse> preview(@PathVariable String code) {
        return ResponseEntity.ok(invitationService.previewInvitation(code));
    }
}
