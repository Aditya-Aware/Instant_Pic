package com.sharedmemory.face;

import com.sharedmemory.common.exception.NotFoundException;
import com.sharedmemory.face.embedding.FaceEmbedder;
import com.sharedmemory.face.matching.EnrollmentResult;
import com.sharedmemory.face.matching.FaceEnrollmentService;
import com.sharedmemory.membership.EventMember;
import com.sharedmemory.membership.MembershipService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FaceProfileService {

    private final FaceProfileRepository faceProfileRepository;
    private final MembershipService membershipService;
    private final FaceEnrollmentService faceEnrollmentService;

    public FaceProfileService(
            FaceProfileRepository faceProfileRepository,
            MembershipService membershipService,
            FaceEnrollmentService faceEnrollmentService) {
        this.faceProfileRepository = faceProfileRepository;
        this.membershipService = membershipService;
        this.faceEnrollmentService = faceEnrollmentService;
    }

    @Transactional
    public FaceProfile enroll(UUID eventId, UUID userId, List<MultipartFile> selfies) {
        EventMember member = membershipService.requireActiveMembership(eventId, userId);
        List<BufferedImage> images = selfies.stream().map(this::readImage).toList();
        EnrollmentResult result = faceEnrollmentService.enroll(images);

        FaceProfile profile = faceProfileRepository.findByEventMemberId(member.getId())
                .orElseGet(() -> FaceProfile.builder().eventMemberId(member.getId()).build());
        profile.setEmbedding(result.embedding());
        profile.setEmbeddingModelVersion(FaceEmbedder.MODEL_VERSION);
        profile.setSampleCount(result.sampleCount());
        profile.setStatus(FaceProfileStatus.ACTIVE);
        return faceProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public Optional<FaceProfile> getStatus(UUID eventId, UUID userId) {
        EventMember member = membershipService.requireActiveMembership(eventId, userId);
        return faceProfileRepository.findByEventMemberId(member.getId())
                .filter(p -> p.getStatus() == FaceProfileStatus.ACTIVE);
    }

    @Transactional
    public void deleteProfile(UUID eventId, UUID userId) {
        EventMember member = membershipService.requireActiveMembership(eventId, userId);
        FaceProfile profile = faceProfileRepository.findByEventMemberId(member.getId())
                .orElseThrow(() -> new NotFoundException("No face profile enrolled for this event"));
        profile.setStatus(FaceProfileStatus.DELETED);
        profile.setEmbedding(new float[0]);
        faceProfileRepository.save(profile);
    }

    private BufferedImage readImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("Uploaded file is not a readable image: " + file.getOriginalFilename());
            }
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded image", e);
        }
    }
}
