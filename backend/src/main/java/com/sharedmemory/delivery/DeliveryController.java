package com.sharedmemory.delivery;

import com.sharedmemory.delivery.dto.DeliveryResponse;
import com.sharedmemory.identity.security.CurrentUserId;
import com.sharedmemory.media.dto.PhotoResponse;
import com.sharedmemory.membership.EventMember;
import com.sharedmemory.membership.MembershipService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/members/me")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final MembershipService membershipService;

    public DeliveryController(DeliveryService deliveryService, MembershipService membershipService) {
        this.deliveryService = deliveryService;
        this.membershipService = membershipService;
    }

    @GetMapping("/my-moments")
    public ResponseEntity<List<PhotoResponse>> myMoments(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        EventMember member = membershipService.requireActiveMembership(eventId, CurrentUserId.get());
        List<PhotoResponse> photos = deliveryService.getMyMoments(member.getId(), PageRequest.of(page, size)).stream()
                .map(PhotoResponse::from)
                .toList();
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/deliveries")
    public ResponseEntity<List<DeliveryResponse>> pendingDeliveries(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        EventMember member = membershipService.requireActiveMembership(eventId, CurrentUserId.get());
        Pageable pageable = PageRequest.of(page, size);
        List<DeliveryResponse> deliveries = deliveryService.getPendingDeliveries(member.getId(), pageable).stream()
                .map(DeliveryResponse::from)
                .toList();
        return ResponseEntity.ok(deliveries);
    }
}
