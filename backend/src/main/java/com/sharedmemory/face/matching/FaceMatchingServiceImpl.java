package com.sharedmemory.face.matching;

import com.sharedmemory.face.FaceMatch;
import com.sharedmemory.face.FaceMatchRepository;
import com.sharedmemory.face.FaceMatchStatus;
import com.sharedmemory.face.FaceProfileRepository;
import com.sharedmemory.face.FaceProfileStatus;
import com.sharedmemory.face.PhotoFace;
import com.sharedmemory.face.PhotoFaceRepository;
import com.sharedmemory.face.api.FaceMatchingFacade;
import com.sharedmemory.face.api.PhotoFaceMatch;
import com.sharedmemory.face.api.PhotoMatchResult;
import com.sharedmemory.media.Photo;
import com.sharedmemory.media.PhotoService;
import com.sharedmemory.membership.EventMember;
import com.sharedmemory.membership.EventMemberRepository;
import com.sharedmemory.membership.MemberStatus;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed implementation of the face.api contract: loads the photo + enrolled candidates,
 * delegates to the pure FaceMatcher for detection/embedding/matching, persists PhotoFace/FaceMatch
 * rows, and returns only participant matches — no sharing decision is made here (see README.md).
 */
@Service
public class FaceMatchingServiceImpl implements FaceMatchingFacade {

    private final PhotoService photoService;
    private final EventMemberRepository eventMemberRepository;
    private final FaceProfileRepository faceProfileRepository;
    private final PhotoFaceRepository photoFaceRepository;
    private final FaceMatchRepository faceMatchRepository;
    private final FaceMatcher faceMatcher;

    public FaceMatchingServiceImpl(
            PhotoService photoService,
            EventMemberRepository eventMemberRepository,
            FaceProfileRepository faceProfileRepository,
            PhotoFaceRepository photoFaceRepository,
            FaceMatchRepository faceMatchRepository,
            FaceMatcher faceMatcher) {
        this.photoService = photoService;
        this.eventMemberRepository = eventMemberRepository;
        this.faceProfileRepository = faceProfileRepository;
        this.photoFaceRepository = photoFaceRepository;
        this.faceMatchRepository = faceMatchRepository;
        this.faceMatcher = faceMatcher;
    }

    @Override
    @Transactional
    public PhotoMatchResult detectAndMatch(UUID photoId) {
        Photo photo = photoService.getById(photoId);
        BufferedImage image = photoService.getDecryptedImage(photoId);

        List<EventMember> activeMembers =
                eventMemberRepository.findByEventIdAndStatus(photo.getEventId(), MemberStatus.ACTIVE);
        Map<UUID, UUID> profileIdToMemberId = new HashMap<>();
        List<MatchCandidate> candidates = new ArrayList<>();
        for (EventMember member : activeMembers) {
            faceProfileRepository.findByEventMemberId(member.getId())
                    .filter(profile -> profile.getStatus() == FaceProfileStatus.ACTIVE)
                    .ifPresent(profile -> {
                        candidates.add(new MatchCandidate(profile.getId(), profile.getEmbedding()));
                        profileIdToMemberId.put(profile.getId(), member.getId());
                    });
        }

        List<DetectedFaceMatch> detectedFaces = faceMatcher.matchAll(image, candidates);

        List<PhotoFaceMatch> matches = new ArrayList<>();
        int unmatchedFaceCount = 0;
        for (DetectedFaceMatch detected : detectedFaces) {
            PhotoFace photoFace = photoFaceRepository.save(PhotoFace.builder()
                    .photoId(photoId)
                    .boundingBoxX(detected.box().x1())
                    .boundingBoxY(detected.box().y1())
                    .boundingBoxW(detected.box().width())
                    .boundingBoxH(detected.box().height())
                    .detectionConfidence(detected.box().confidence())
                    .embedding(detected.embedding())
                    .build());

            if (detected.matchedProfileId().isPresent()) {
                UUID profileId = detected.matchedProfileId().get();
                UUID memberId = profileIdToMemberId.get(profileId);
                faceMatchRepository.save(FaceMatch.builder()
                        .photoFaceId(photoFace.getId())
                        .eventMemberId(memberId)
                        .faceProfileId(profileId)
                        .similarityScore(detected.bestScore())
                        .matchStatus(FaceMatchStatus.CONFIRMED)
                        .matchedAt(Instant.now())
                        .build());
                matches.add(new PhotoFaceMatch(photoFace.getId(), memberId, detected.bestScore()));
            } else {
                unmatchedFaceCount++;
            }
        }

        return new PhotoMatchResult(
                photoId, photo.getEventId(), photo.getUploadedByMemberId(), matches, unmatchedFaceCount);
    }
}
