package com.sharedmemory.event;

import com.sharedmemory.event.dto.CreateEventRequest;
import com.sharedmemory.event.dto.EventResponse;
import com.sharedmemory.identity.User;
import com.sharedmemory.identity.UserRepository;
import com.sharedmemory.identity.security.CurrentUserId;
import com.sharedmemory.membership.MembershipService;
import com.sharedmemory.rules.SharingRuleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final MembershipService membershipService;
    private final UserRepository userRepository;
    private final SharingRuleService sharingRuleService;

    public EventController(
            EventService eventService,
            MembershipService membershipService,
            UserRepository userRepository,
            SharingRuleService sharingRuleService) {
        this.eventService = eventService;
        this.membershipService = membershipService;
        this.userRepository = userRepository;
        this.sharingRuleService = sharingRuleService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        UUID userId = CurrentUserId.get();
        Event event = eventService.createEvent(userId, request);
        User creator = userRepository.findById(userId).orElseThrow();
        membershipService.addOrganizer(event.getId(), userId, creator.getDisplayName());
        sharingRuleService.seedDefaultRules(event.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(event));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> listMyEvents() {
        List<UUID> eventIds = membershipService.listActiveEventIdsForUser(CurrentUserId.get());
        List<EventResponse> events = eventService.getAllByIds(eventIds).stream()
                .map(EventResponse::from)
                .toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> get(@PathVariable UUID eventId) {
        membershipService.requireActiveMembership(eventId, CurrentUserId.get());
        return ResponseEntity.ok(EventResponse.from(eventService.getById(eventId)));
    }
}
