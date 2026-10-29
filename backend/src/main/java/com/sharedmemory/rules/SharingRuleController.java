package com.sharedmemory.rules;

import com.sharedmemory.identity.security.CurrentUserId;
import com.sharedmemory.membership.MemberRole;
import com.sharedmemory.membership.MembershipService;
import com.sharedmemory.rules.dto.SharingRuleResponse;
import com.sharedmemory.rules.dto.UpdateSharingRuleRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/sharing-rules")
public class SharingRuleController {

    private final SharingRuleService sharingRuleService;
    private final MembershipService membershipService;

    public SharingRuleController(SharingRuleService sharingRuleService, MembershipService membershipService) {
        this.sharingRuleService = sharingRuleService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public ResponseEntity<List<SharingRuleResponse>> list(@PathVariable UUID eventId) {
        membershipService.requireActiveMembership(eventId, CurrentUserId.get());
        return ResponseEntity.ok(sharingRuleService.list(eventId).stream().map(SharingRuleResponse::from).toList());
    }

    @PutMapping
    public ResponseEntity<SharingRuleResponse> update(
            @PathVariable UUID eventId, @Valid @RequestBody UpdateSharingRuleRequest request) {
        membershipService.requireRole(eventId, CurrentUserId.get(), MemberRole.ORGANIZER);
        RuleType ruleType = RuleType.valueOf(request.ruleType());
        SharingRule updated = sharingRuleService.setEnabled(eventId, ruleType, request.enabled());
        return ResponseEntity.ok(SharingRuleResponse.from(updated));
    }
}
