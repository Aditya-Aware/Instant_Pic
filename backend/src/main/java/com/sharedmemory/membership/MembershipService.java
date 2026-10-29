package com.sharedmemory.membership;

import com.sharedmemory.common.exception.ForbiddenException;
import com.sharedmemory.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

    private final EventMemberRepository eventMemberRepository;
    private final InvitationService invitationService;

    public MembershipService(EventMemberRepository eventMemberRepository, InvitationService invitationService) {
        this.eventMemberRepository = eventMemberRepository;
        this.invitationService = invitationService;
    }

    @Transactional
    public EventMember addOrganizer(UUID eventId, UUID userId, String displayName) {
        EventMember member = EventMember.builder()
                .eventId(eventId)
                .userId(userId)
                .displayName(displayName)
                .role(MemberRole.ORGANIZER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
        return eventMemberRepository.save(member);
    }

    @Transactional
    public EventMember joinByInvitationCode(UUID userId, String code, String displayName) {
        Invitation invitation = invitationService.getValidInvitation(code);

        var existing = eventMemberRepository.findByEventIdAndUserId(invitation.getEventId(), userId);
        if (existing.isPresent() && existing.get().getStatus() == MemberStatus.ACTIVE) {
            return existing.get();
        }

        invitationService.consumeUse(invitation);

        if (existing.isPresent()) {
            EventMember member = existing.get();
            member.setStatus(MemberStatus.ACTIVE);
            member.setDisplayName(displayName);
            member.setJoinedAt(Instant.now());
            member.setLeftAt(null);
            return eventMemberRepository.save(member);
        }

        EventMember member = EventMember.builder()
                .eventId(invitation.getEventId())
                .userId(userId)
                .displayName(displayName)
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
        return eventMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<EventMember> listActiveMembers(UUID eventId, UUID requestingUserId) {
        requireActiveMembership(eventId, requestingUserId);
        return eventMemberRepository.findByEventIdAndStatus(eventId, MemberStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<UUID> listActiveEventIdsForUser(UUID userId) {
        return eventMemberRepository.findByUserIdAndStatus(userId, MemberStatus.ACTIVE)
                .stream()
                .map(EventMember::getEventId)
                .toList();
    }

    @Transactional
    public void leaveEvent(UUID eventId, UUID userId) {
        EventMember member = requireActiveMembership(eventId, userId);
        member.setStatus(MemberStatus.LEFT);
        member.setLeftAt(Instant.now());
        eventMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(UUID eventId, UUID organizerUserId, UUID targetMemberId) {
        requireRole(eventId, organizerUserId, MemberRole.ORGANIZER);
        EventMember target = eventMemberRepository.findById(targetMemberId)
                .filter(m -> m.getEventId().equals(eventId))
                .orElseThrow(() -> new NotFoundException("Member not found"));
        target.setStatus(MemberStatus.REMOVED);
        target.setLeftAt(Instant.now());
        eventMemberRepository.save(target);
    }

    @Transactional(readOnly = true)
    public EventMember requireActiveMembership(UUID eventId, UUID userId) {
        return eventMemberRepository.findByEventIdAndUserId(eventId, userId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("Not an active member of this event"));
    }

    @Transactional(readOnly = true)
    public EventMember requireRole(UUID eventId, UUID userId, MemberRole requiredRole) {
        EventMember member = requireActiveMembership(eventId, userId);
        if (member.getRole() != requiredRole) {
            throw new ForbiddenException("This action requires the " + requiredRole + " role");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public boolean isOptedOut(UUID eventId, UUID userId) {
        return eventMemberRepository.findByEventIdAndUserId(eventId, userId)
                .map(m -> m.getStatus() != MemberStatus.ACTIVE)
                .orElse(true);
    }
}
