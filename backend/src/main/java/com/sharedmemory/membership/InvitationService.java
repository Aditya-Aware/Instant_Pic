package com.sharedmemory.membership;

import com.sharedmemory.common.exception.ConflictException;
import com.sharedmemory.common.exception.ForbiddenException;
import com.sharedmemory.common.exception.NotFoundException;
import com.sharedmemory.event.Event;
import com.sharedmemory.event.EventRepository;
import com.sharedmemory.membership.dto.InvitationPreviewResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventRepository eventRepository;
    private final int defaultTtlDays;

    public InvitationService(
            InvitationRepository invitationRepository,
            EventMemberRepository eventMemberRepository,
            EventRepository eventRepository,
            @Value("${app.invitation.default-ttl-days}") int defaultTtlDays) {
        this.invitationRepository = invitationRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.eventRepository = eventRepository;
        this.defaultTtlDays = defaultTtlDays;
    }

    @Transactional
    public Invitation generateInvitation(UUID eventId, UUID requestingUserId) {
        EventMember member = eventMemberRepository.findByEventIdAndUserId(eventId, requestingUserId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("Not an active member of this event"));
        if (member.getRole() != MemberRole.ORGANIZER) {
            throw new ForbiddenException("Only the event organizer can create invitations");
        }
        Invitation invitation = Invitation.builder()
                .eventId(eventId)
                .code(generateUniqueCode())
                .createdByMemberId(member.getId())
                .expiresAt(Instant.now().plus(defaultTtlDays, ChronoUnit.DAYS))
                .maxUses(null)
                .usesCount(0)
                .status(InvitationStatus.ACTIVE)
                .build();
        return invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public InvitationPreviewResponse previewInvitation(String code) {
        Invitation invitation = getValidInvitation(code);
        Event event = eventRepository.findById(invitation.getEventId())
                .orElseThrow(() -> new NotFoundException("Event not found"));
        String organizerName = eventMemberRepository.findByEventIdAndStatus(event.getId(), MemberStatus.ACTIVE)
                .stream()
                .filter(m -> m.getRole() == MemberRole.ORGANIZER)
                .map(EventMember::getDisplayName)
                .findFirst()
                .orElse("Organizer");
        long memberCount = eventMemberRepository.findByEventIdAndStatus(event.getId(), MemberStatus.ACTIVE).size();
        return new InvitationPreviewResponse(
                event.getId(), event.getName(), organizerName, memberCount, invitation.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public Invitation getValidInvitation(String code) {
        Invitation invitation = invitationRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
        if (invitation.getStatus() != InvitationStatus.ACTIVE || invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Invitation is no longer valid");
        }
        if (invitation.getMaxUses() != null && invitation.getUsesCount() >= invitation.getMaxUses()) {
            throw new ConflictException("Invitation has reached its maximum uses");
        }
        return invitation;
    }

    @Transactional
    public void consumeUse(Invitation invitation) {
        invitation.setUsesCount(invitation.getUsesCount() + 1);
        invitationRepository.save(invitation);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (invitationRepository.findByCode(code).isPresent());
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
