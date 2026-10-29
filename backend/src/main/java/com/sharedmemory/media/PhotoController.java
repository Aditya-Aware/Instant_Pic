package com.sharedmemory.media;

import com.sharedmemory.common.exception.ForbiddenException;
import com.sharedmemory.delivery.DeliveryRepository;
import com.sharedmemory.identity.security.CurrentUserId;
import com.sharedmemory.media.dto.PhotoResponse;
import com.sharedmemory.membership.EventMember;
import com.sharedmemory.membership.MembershipService;
import com.sharedmemory.photoprocessing.PhotoProcessingOrchestrator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PhotoController {

    private final PhotoService photoService;
    private final MembershipService membershipService;
    private final DeliveryRepository deliveryRepository;
    private final PhotoProcessingOrchestrator photoProcessingOrchestrator;

    public PhotoController(
            PhotoService photoService,
            MembershipService membershipService,
            DeliveryRepository deliveryRepository,
            PhotoProcessingOrchestrator photoProcessingOrchestrator) {
        this.photoService = photoService;
        this.membershipService = membershipService;
        this.deliveryRepository = deliveryRepository;
        this.photoProcessingOrchestrator = photoProcessingOrchestrator;
    }

    @PostMapping("/api/events/{eventId}/photos")
    public ResponseEntity<PhotoResponse> upload(
            @PathVariable UUID eventId, @RequestParam("photo") MultipartFile photo) {
        Photo uploaded = photoService.upload(eventId, CurrentUserId.get(), photo);
        photoProcessingOrchestrator.process(uploaded.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PhotoResponse.from(photoService.getById(uploaded.getId())));
    }

    @PostMapping("/api/events/{eventId}/photos/{photoId}/process")
    public ResponseEntity<PhotoResponse> process(@PathVariable UUID eventId, @PathVariable UUID photoId) {
        membershipService.requireActiveMembership(eventId, CurrentUserId.get());
        photoProcessingOrchestrator.process(photoId);
        return ResponseEntity.ok(PhotoResponse.from(photoService.getById(photoId)));
    }

    @GetMapping("/api/events/{eventId}/photos/{photoId}")
    public ResponseEntity<PhotoResponse> detail(@PathVariable UUID eventId, @PathVariable UUID photoId) {
        membershipService.requireActiveMembership(eventId, CurrentUserId.get());
        return ResponseEntity.ok(PhotoResponse.from(photoService.getById(photoId)));
    }

    @GetMapping("/api/events/{eventId}/gallery")
    public ResponseEntity<List<PhotoResponse>> gallery(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<PhotoResponse> photos = photoService.getGallery(eventId, CurrentUserId.get(), pageable).stream()
                .map(PhotoResponse::from)
                .toList();
        return ResponseEntity.ok(photos);
    }

    @GetMapping(value = "/api/photos/{photoId}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> image(@PathVariable UUID photoId) {
        Photo photo = photoService.getById(photoId);
        EventMember member = membershipService.requireActiveMembership(photo.getEventId(), CurrentUserId.get());
        boolean authorized = photo.isGalleryVisible()
                || photo.getUploadedByMemberId().equals(member.getId())
                || deliveryRepository.findByPhotoIdAndRecipientMemberId(photoId, member.getId()).isPresent();
        if (!authorized) {
            throw new ForbiddenException("Not authorized to view this photo");
        }
        BufferedImage image = photoService.getDecryptedImage(photoId);
        return ResponseEntity.ok(encodeJpeg(image));
    }

    private byte[] encodeJpeg(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode image", e);
        }
    }
}
